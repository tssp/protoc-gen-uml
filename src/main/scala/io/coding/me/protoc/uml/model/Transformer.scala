package io.coding.me.protoc.uml.model

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.DescriptorProtos._
import com.google.protobuf.Descriptors._
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import scala.collection.JavaConverters._

import io.coding.me.protoc.uml.util.NameFormatter._

/** Takes a PB model in form of a CodeGeneratorRequest and transforms it into a simplified model.   */
object Transformer {

  /** Main entry point - simply transform. */
  def apply(request: CodeGeneratorRequest): TypeRepository =
    request.getProtoFileList.asScala
      .foldLeft[Map[String, FileDescriptor]](Map.empty) {
        case (acc, fp: FileDescriptorProto) =>
          val deps = fp.getDependencyList.asScala.map(dp => acc.apply(dp))
          acc + (fp.getName -> FileDescriptor.buildFrom(fp, deps.toArray))
      }
      .flatMap {
        case (file, fileDescriptor) =>
          transformFileDescriptor(fileDescriptor, Types.Origin(file))
      }
      .map(typ => typ.identifier -> typ)
      .toMap

  import io.coding.me.protoc.uml.model.FieldTypes._
  import io.coding.me.protoc.uml.model.Multiplicities._
  import io.coding.me.protoc.uml.model.MessageFields._
  import io.coding.me.protoc.uml.model.Types._

  /** Transform on file level. */
  private[model] def transformFileDescriptor(fileDescriptor: FileDescriptor, origin: Types.Origin): Set[Types.Type] =
    fileDescriptor.getMessageTypes.asScala.flatMap(message => transformMessageDescriptor(message, None, origin)).toSet ++
      fileDescriptor.getEnumTypes.asScala.map(enum => transformEnumDescriptor(enum, None, origin)).toSet

  /** Transform on message level */
  private[model] def transformMessageDescriptor(md: Descriptor, enclosingType: Option[TypeIdentifier], origin: Types.Origin): Set[Types.Type] = {

    val identifier = getMessageIdentifier(md, enclosingType)

    val (typedFieldDescriptors, oneOfFieldDescriptors) = md.getFields.asScala.partition(_.getContainingOneof == null)

    val typedFields = typedFieldDescriptors.map(transformFieldDescriptor)

    val extensionsFields =
      (md.getFile.getExtensions.asScala.filter(fd => getMessageIdentifierHierarchy(fd.getContainingType) == identifier) ++ md.getExtensions.asScala)
        .map(transformFieldDescriptor)
        .distinct

    val nonAncestorExtensionFields = extensionsFields

    val oneOfTypes = oneOfFieldDescriptors
      .map(_.getContainingOneof)
      .distinct
      .map { oneOfDescriptor =>
        val oneOfName       = oneOfDescriptor.getName.toCamelCase
        val oneOfFieldName  = oneOfDescriptor.getName
        val oneOfIdentifier = identifier.copy(name = Name(s"${identifier.name.n}${TYPE_NAME_SEPARATOR}${oneOfName}"))
        val oneOfFields     = oneOfDescriptor.getFields.asScala.map(transformFieldDescriptor)

        OneOfType(oneOfIdentifier, Some(identifier), oneOfFieldName, oneOfFields, origin)
      }
      .toSet

    val oneOfFields = oneOfTypes.map(oneOfType => OneOfField(oneOfType.fieldName, oneOfType.identifier))

    val fields = typedFields ++ oneOfFields ++ nonAncestorExtensionFields

    val messageType = Types.MessageType(identifier, enclosingType, fields, origin)

    val nestedEnumTypes    = md.getEnumTypes.asScala.map(nested => transformEnumDescriptor(nested, Some(identifier), origin)).toSet
    val nestedMessageTypes = md.getNestedTypes.asScala.flatMap(nested => transformMessageDescriptor(nested, Some(identifier), origin)).toSet

    // Due to backwards compatibility in PB, maps become a nested type (see also https://developers.google.com/protocol-buffers/docs/proto3#maps).
    // This is here not desired. Therefore filter them out.
    val nestedMessageTypesWO = nestedMessageTypes.filterNot {

      case Types.MessageType(identifier, Some(enclosingType), elements, _) =>
        // TODO: Improved algorithm to determine artificial map type
        elements.size == 2 && elements.exists(_.name == "key") && elements.exists(_.name == "value") && identifier.name.toString.endsWith("Entry")

      case _ => true
    }

    oneOfTypes ++ nestedEnumTypes ++ nestedMessageTypesWO ++ Set(messageType)

  }

  /** Transform on field type level */
  private[model] def transformFieldType(fieldType: FieldDescriptor.Type): FieldTypes.FieldType =
    fieldType match {

      case Type.STRING => FieldTypes.ScalarValueType("String")
      case Type.FLOAT  => FieldTypes.ScalarValueType("Float")
      case Type.DOUBLE => FieldTypes.ScalarValueType("Double")
      case Type.BYTES  => FieldTypes.ScalarValueType("Bytes")
      case Type.BOOL   => FieldTypes.ScalarValueType("Bool")
      case Type.INT32 | Type.UINT32 | Type.SINT32 | Type.FIXED32 | Type.SFIXED32 =>
        FieldTypes.ScalarValueType("Int")
      case Type.INT64 | Type.UINT64 | Type.SINT64 | Type.FIXED64 | Type.SFIXED64 =>
        FieldTypes.ScalarValueType("Long")
      case Type.MESSAGE => ???
      case Type.ENUM    => ???
      case Type.GROUP   => ???
    }

  /** Transform on field level */
  private[model] def transformFieldDescriptor(fd: FieldDescriptor): MessageFields.TypedField = {

    val name = fd.getName

    val multiplicity =
      if (fd.isMapField) None
      else if (fd.isRequired) Some(Required)
      else if (fd.isOptional) Some(Optional)
      else if (fd.isRepeated) Some(Many)
      else None

    val fieldType = if (fd.isMapField) {
      val messageType = fd.getMessageType

      val keyType   = transformFieldDescriptor(messageType.getFields.asScala.find(_.getName == "key").get).fieldType
      val valueType = transformFieldDescriptor(messageType.getFields.asScala.find(_.getName == "value").get).fieldType

      FieldTypes.MapType(keyType, valueType)

    } else if (fd.getType == Type.MESSAGE) FieldTypes.CompoundType(getMessageIdentifierHierarchy(fd.getMessageType))
    else if (fd.getType == Type.ENUM) FieldTypes.CompoundType(getEnumIdentifierHierarchy(fd.getEnumType))
    else transformFieldType(fd.getType)

    MessageFields.TypedField(name, fieldType, multiplicity)
  }

  /** Transform on enum level */
  private[model] def transformEnumDescriptor(ed: EnumDescriptor, enclosingType: Option[TypeIdentifier], origin: Types.Origin): Types.Type = {

    val identifier = getEnumIdentifier(ed, enclosingType)
    val enumFields = ed.getValues.asScala.map(evd => evd.getName).map(EnumFields.EnumField)
    val enumType   = Types.EnumType(identifier, enclosingType, enumFields, origin)

    enumType
  }

  /** Generate a unique (?) type identifier based on package name, type name and enclosing type. */
  private[model] def getTypeIdentifier(typeName: String, fullName: String, enclosingType: Option[TypeIdentifier]): TypeIdentifier = {

    val (name, pakkage) = enclosingType.map { enclosing =>
      val n = Name(s"${enclosing.name}${TYPE_NAME_SEPARATOR}${typeName}")
      val p = enclosing.pakkage

      (n, p)
    }.getOrElse {

      val n = Name(typeName)
      val p = Package(fullName.replaceAll(s".${typeName}", "")) // Last element points to :wtype name

      (n, p)
    }
    TypeIdentifier(pakkage, name)
  }

  /** Generate a unique (?) type identifier based on package name, type name and enclosing type. */
  private[model] def getEnumIdentifier(ed: EnumDescriptor, enclosingType: Option[TypeIdentifier]): TypeIdentifier =
    getTypeIdentifier(ed.getName, ed.getFullName, enclosingType)

  /** Generate a unique (?) type identifier based on package name, type name and enclosing type. */
  private[model] def getMessageIdentifier(md: Descriptor, enclosingType: Option[TypeIdentifier]): TypeIdentifier =
    getTypeIdentifier(md.getName, md.getFullName, enclosingType)

  /** Builds an identifier for a leaf enum - tracing the path to its root. */
  private[model] def getEnumIdentifierHierarchy(ed: EnumDescriptor): TypeIdentifier =
    if (ed.getContainingType == null) getEnumIdentifier(ed, None)
    else {

      val parentTypeIdentifier = getMessageIdentifierHierarchy(ed.getContainingType)
      val enumTypeIdentifier   = parentTypeIdentifier.copy(name = Name(s"${parentTypeIdentifier.name.n}${TYPE_NAME_SEPARATOR}${ed.getName}"))

      enumTypeIdentifier
    }

  /** Builds an identifier for a leaf message - tracing the path to its root. */
  private[model] def getMessageIdentifierHierarchy(md: Descriptor): TypeIdentifier = {

    @scala.annotation.tailrec
    def buildTypeList(md: Descriptor, acc: Seq[Descriptor]): Seq[Descriptor] =
      if (md == null) acc
      else buildTypeList(md.getContainingType, md +: acc)

    val types          = buildTypeList(md, Nil)
    val rootIdentifier = getMessageIdentifier(types.head, None)
    val identifier     = types.tail.foldLeft(rootIdentifier)((enclosingType, typ) => getMessageIdentifier(typ, Some(enclosingType)))

    identifier
  }
}
