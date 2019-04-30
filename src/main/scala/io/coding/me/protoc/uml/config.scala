package io.coding.me.protoc.uml

import pureconfig.{CamelCase, ConfigFieldMapping}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.ConfigReader.fromString

package object config {

  /** Configures the representation of OneOf fields */
  object OneOfRepresentation extends Enumeration {

    /** Creates a class with stereotype <oneOf> for each oneOf field */
    val SEPARATE_TYPE = Value("SeparateType")

    /** Integrate fields of oneOf field into class */
    val INTEGRATE_FIELD = Value("IntegrateField")
  }

  /** Configures the output format */
  object OutputFormat extends Enumeration {

    val PLANT_UML = Value("PlantUML")
  }

  /** Additional grouing inside output file */
  object OutputGrouping extends Enumeration {

    val DEFAULT = Value("Default")
    val BY_FILE = Value("ByFile")
  }

  /** Configures which types shall not be part of the output. */
  case class OutputFilter(packages: Set[String])

  /** Configures the organization of the output files */
  object OutputFileOrganization extends Enumeration {

    /** Each input file corresponds to one output file */
    val DIRECT_MAPPING = Value("DirectMapping")

    /** Only one large file will be created */
    val SINGLE_FILE = Value("SingleFile")

    /** Create one file per package */
    val FILE_PER_PACKAGE = Value("FilePerPackage")
  }

  case class View(pakkage: Boolean, fields: Boolean, relations: Boolean)
  case class PlantUMLConfig(fileHeader: String)
  case class Formatter(plantUML: PlantUMLConfig)
  case class UML(formatter: Formatter, view: View)

  case class Output(format: OutputFormat.Value, organization: OutputFileOrganization.Value, grouping: OutputGrouping.Value, file: String, filter: OutputFilter)
  case class Config(output: Output, uml: UML)

  implicit val converterOutputFileOrganization = fromString[OutputFileOrganization.Value](s => Right(OutputFileOrganization.withName(s)))
  implicit val converterOutputGrouping         = fromString[OutputGrouping.Value](s => Right(OutputGrouping.withName(s)))
  implicit val converterOutputFormat           = fromString[OutputFormat.Value]( s => Right(OutputFormat.withName(s)))
  implicit val converterOneOfRepresentation    = fromString[OneOfRepresentation.Value](s => Right(OneOfRepresentation.withName(s)))

  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  object Configuration {

    def apply(): Config = pureconfig.loadConfig[Config]("protoc-gen-uml").right.get
  }
}
