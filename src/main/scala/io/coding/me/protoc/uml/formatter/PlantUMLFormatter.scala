package io.coding.me.protoc.uml.formatter

import io.coding.me.protoc.uml.model._
import io.coding.me.protoc.uml.model.FieldTypes._
import io.coding.me.protoc.uml.model.MessageFields._
import io.coding.me.protoc.uml.model.Multiplicities._

import io.coding.me.protoc.uml.{config => c}

/** Generates a textual description of the Protos in PlantUML format. */
object PlantUMLFormatter extends UMLFormatter {

  override def apply(types: Iterable[Types.Type], typeRepository: TypeRepository, config: c.Config) = {

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
      case _          => ""
    }
    def formatField(field: MessageField, pakkage: Package, enclosingType: TypeIdentifier) = field match {

      case OneOfField(name, typeIdentifier)          => s"$name: ${formatFullTypeName(typeIdentifier, pakkage, enclosingType)}"
      case TypedField(name, fieldType, multiplicity) => s"$name: ${formatFieldType(fieldType, pakkage, enclosingType)} ${formatMultiplicity(multiplicity)}"
      case _                                         => ???
    }

    def formatType(typ: Types.Type, pakkage: Package) =
      StructuredStringFormatter.add {
        typ match {

          case e: Types.EnumType    => s"enum ${e.identifier.name}"
          case o: Types.OneOfType   => s"class ${o.identifier.name} << oneOf >>"
          case m: Types.MessageType => s"class ${m.identifier.name}"
        }
      }.withCondition(config.uml.view.fields) {
          _.withCurlyBrackets { f =>
            typ match {

              case e: Types.EnumType    => formatIterable(f, e.elements.map(_.name))
              case o: Types.OneOfType   => formatIterable(f, o.elements.map(f => formatField(f, pakkage, o.identifier)))
              case m: Types.MessageType => formatIterable(f, m.elements.map(f => formatField(f, pakkage, m.identifier)))
            }
          }

        }
        .newline

    def formatTypes(types: Iterable[Types.Type]) = StructuredStringFormatter.add {

      config.output.grouping match {

        case c.OutputGrouping.BY_FILE =>
          types.groupBy(_.origin.file).map {
            case (_, typesPerFile) =>
              StructuredStringFormatter
                .add("together")
                .withCurlyBrackets(_.add(typesPerFile.toSeq.sortBy(_.identifier.name.n).map(t => formatType(t, t.identifier.pakkage))))
          }

        case _ => types.toSeq.sortBy(_.identifier.name.n).map(t => formatType(t, t.identifier.pakkage))

      }
    }

    StructuredStringFormatter
      .add("@startuml")
      .add(config.uml.formatter.plantUML.fileHeader)
      .withCondition(config.uml.view.relations) {
        _.add {

          types.map { typ =>
            val fromName  = typ.identifier.toString
            val relations = typ.referencedTypeIdentifiers.map(_.toString).map(toName => s"$fromName -- $toName")

            formatIterable(StructuredStringFormatter.newline, relations)
          }
        }
      }
      .withIfElse(config.uml.view.pakkage)(ifFormatter = {

        _.add(types.groupBy(_.identifier.pakkage).map {
          case (pakkage, typesPerPackage) =>
            StructuredStringFormatter.add(s"package $pakkage").withCurlyBrackets(_.add(formatTypes(typesPerPackage)))
        })

      }, elseFormatter = _.add(formatTypes(types)))
      .add("@enduml")
      .toString

  }

}
