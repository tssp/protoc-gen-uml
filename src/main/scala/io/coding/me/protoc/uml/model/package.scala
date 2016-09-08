package io.coding.me.protoc.uml

/** This package contains all types of the simplified protobuf model.
  *
  *  To not deal with all the bells and whistles of the protobuf model that shall be compiled, it will
  *  be translated to a simplified model which should then be used by the different UML implementations.
  *
  */
package object model {

  val TYPE_NAME_SEPARATOR = "::"

  case class Package(p: String) {
    require(p != null && p.nonEmpty)

    override lazy val toString = p
  }

  case class Name(n: String) {
    require(n != null && n.nonEmpty)

    override lazy val toString = n
  }

  case class TypeIdentifier(pakkage: Package, name: Name) {

    override lazy val toString = s"${pakkage.p}.${name.n}"
  }

  type TypeRepository = Map[TypeIdentifier, Types.Type]

  object FieldTypes {

    sealed trait FieldType

    /** Refers to https://developers.google.com/protocol-buffers/docs/proto#scalar  */
    case class ScalarValueType(name: String) extends FieldType

    /** Refers to https://developers.google.com/protocol-buffers/docs/proto#maps */
    case class MapType(keyType: FieldType, valueType: FieldType) extends FieldType

    /** Refers to user-defined message/enumeration types */
    case class CompoundType(identifier: TypeIdentifier) extends FieldType

  }

  object Multiplicities {

    sealed trait Multiplicity

    case object Many     extends Multiplicity
    case object Optional extends Multiplicity
    case object Required extends Multiplicity
  }

  object EnumFields {

    case class EnumField(name: String)
  }

  object MessageFields {

    sealed trait MessageField {

      def name: String
    }

    case class TypedField(name: String, fieldType: FieldTypes.FieldType, multiplicity: Option[Multiplicities.Multiplicity]) extends MessageField

    /** Represents a PB one-of field.
      *
      * When parsing an one-of field, it becomes a new [[Types.OneOfType]] containing all the fields that are
      * part of the one-of group. The idea behind is to generate a new UML stereotype for one-of fields to
      * properly reflect them in a diagram.
      */
    case class OneOfField(name: String, referenceType: TypeIdentifier) extends MessageField
  }

  object Types {

    case class Origin(file: String)

    sealed trait Type {

      type ElementType

      def identifier: TypeIdentifier
      def enclosingType: Option[TypeIdentifier]
      def elements: Seq[ElementType]

      def origin: Origin
    }

    case class MessageType(identifier: TypeIdentifier, enclosingType: Option[TypeIdentifier], elements: Seq[MessageFields.MessageField], origin: Origin)
        extends Type {

      type ElementType = MessageFields.MessageField
    }

    case class EnumType(identifier: TypeIdentifier, enclosingType: Option[TypeIdentifier], elements: Seq[EnumFields.EnumField], origin: Origin) extends Type {

      type ElementType = EnumFields.EnumField
    }

    case class OneOfType(identifier: TypeIdentifier,
                         enclosingType: Option[TypeIdentifier],
                         fieldName: String,
                         elements: Seq[MessageFields.MessageField],
                         origin: Origin)
        extends Type {
      require(enclosingType.isDefined)

      type ElementType = MessageFields.MessageField
    }
  }

}
