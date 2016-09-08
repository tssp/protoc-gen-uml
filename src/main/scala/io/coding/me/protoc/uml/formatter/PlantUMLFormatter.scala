package io.coding.me.protoc.uml.formatter

import io.coding.me.protoc.uml.model._
import io.coding.me.protoc.uml.model.FieldTypes._
import io.coding.me.protoc.uml.model.MessageFields._
import io.coding.me.protoc.uml.model.Multiplicities._

import io.coding.me.protoc.uml.{config => c}

/** Generates a textual description of the Protos in PlantUML format. */
object PlantUMLFormatter extends UMLFormatter {

  override def apply(types: Iterable[Types.Type], typeRepository: TypeRepository, config: c.UML) = {

    def formatIterable[T <: String](initial: StructuredStringFormatter, iterable: Iterable[T]) =
      iterable.foldLeft(initial)((formatter, element) => formatter.add(element))

    def formatFullTypeName(typeIdentifier: TypeIdentifier, pakkage: Package, enclosingType: TypeIdentifier): String =
      if (typeIdentifier.pakkage == pakkage || typeIdentifier.pakkage == enclosingType.pakkage)
        typeIdentifier.name.toString
      else typeIdentifier.toString

    def formatFullTypeNameWithPackage(typeIdentifier: TypeIdentifier, pakkage: Package): String =
      if (typeIdentifier.pakkage == pakkage) typeIdentifier.name.toString else typeIdentifier.toString

    def formatFieldType(fieldType: FieldType, pakkage: Package, enclosingType: TypeIdentifier): String = fieldType match {

      case ScalarValueType(scalar)      => scalar
      case CompoundType(typeIdentifier) => formatFullTypeName(typeIdentifier, pakkage, enclosingType)
      case MapType(keyFieldType, valueFieldType) =>
        val keyFieldName   = formatFieldType(keyFieldType, pakkage, enclosingType)
        val valueFieldName = formatFieldType(valueFieldType, pakkage, enclosingType)
        s"Map<$keyFieldName,$valueFieldName>"

      case _ => ???
    }

    def formatMultiplicity(m: Option[Multiplicity]) = m match {

      case Some(Many) => "[*]"
      case _ => ""
    }
    def formatField(field: MessageField, pakkage: Package, enclosingType: TypeIdentifier) = field match {

      case OneOfField(name, typeIdentifier)         => s"$name: ${formatFullTypeName(typeIdentifier, pakkage, enclosingType)}"
      case TypedField(name, fieldType, multiplicity) => s"$name: ${formatFieldType(fieldType, pakkage, enclosingType)} ${formatMultiplicity(multiplicity)}"
      case _                                        => ???
    }

    def formatType(typ: Types.Type, pakkage: Package) =
      StructuredStringFormatter.add {
        typ match {

          case e: Types.EnumType    => s"enum ${e.identifier.name}"
          case o: Types.OneOfType   => s"class ${o.identifier.name} << oneOf >>"
          case m: Types.MessageType => s"class ${m.identifier.name}"
        }
      }.withCondition(config.view.fields) {
          _.withCurlyBrackets { f =>
            typ match {

              case e: Types.EnumType    => formatIterable(f, e.elements.map(_.name))
              case o: Types.OneOfType   => formatIterable(f, o.elements.map(f => formatField(f, pakkage, o.identifier)))
              case m: Types.MessageType => formatIterable(f, m.elements.map(f => formatField(f, pakkage, m.identifier)))
            }
          }

        }
        .withCondition(config.view.relations) { f =>
          val elements = typ match {

            case o: Types.OneOfType   => o.elements
            case m: Types.MessageType => m.elements
            case _                    => Nil
          }

          def findReferencesInFieldType(fieldType: FieldTypes.FieldType): Seq[TypeIdentifier] = fieldType match {

            case CompoundType(typeIdentifier)          => Seq(typeIdentifier)
            case MapType(keyFieldType, valueFieldType) => findReferencesInFieldType(keyFieldType) ++ findReferencesInFieldType(valueFieldType)
            case _                                     => Nil
          }
          def findReferencesInMessageField(messageField: MessageFields.MessageField): Seq[TypeIdentifier] = messageField match {

            case TypedField(_, fieldType, _)   => findReferencesInFieldType(fieldType)
            case OneOfField(_, typeIdentifier) => Seq(typeIdentifier)
            case _                             => Nil

          }
          val referenceElements = elements.flatMap(findReferencesInMessageField)
          val fromName = formatFullTypeNameWithPackage(typ.identifier, pakkage)
          val relations = referenceElements.map(e => formatFullTypeName(e, pakkage, typ.identifier)).distinct.map(toName => s"$fromName -- $toName")

          formatIterable(f, relations)

        }
        .newline

    StructuredStringFormatter
      .add("@startuml")
      .add(config.formatter.plantUML.fileHeader)
      .withIfElse(config.view.pakkage)(ifFormatter = {

        _.add(types.groupBy(_.identifier.pakkage).map {
          case (pakkage, typesPerPackage) =>
            StructuredStringFormatter.add(s"package $pakkage").withCurlyBrackets(_.add(typesPerPackage.map(t => formatType(t, t.identifier.pakkage))))
        })

      }, elseFormatter = _.add(types.map(t => formatType(t, t.identifier.pakkage))))
      .add("@enduml")
      .toString

  }

}
