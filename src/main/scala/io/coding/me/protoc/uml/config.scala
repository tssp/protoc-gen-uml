package io.coding.me.protoc.uml

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.EnumerationReader._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import com.typesafe.config.ConfigFactory

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

  case class Output(format: OutputFormat.Value, organization: OutputFileOrganization.Value, file: String)
  case class Config(output: Output, uml: UML)

  object Configuration {

    def apply(): Config = ConfigFactory.load.as[Config]("protoc-gen-uml")
  }
}
