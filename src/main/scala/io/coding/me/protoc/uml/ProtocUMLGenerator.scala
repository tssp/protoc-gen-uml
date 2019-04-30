package io.coding.me.protoc.uml

import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import io.coding.me.protoc.uml.config._
import io.coding.me.protoc.uml.formatter._
import io.coding.me.protoc.uml.model._
import protocbridge.ProtocCodeGenerator

import scala.collection.JavaConverters._

class ProtocUMLGenerator(config: Config) extends ProtocCodeGenerator {

  var typeRepository: TypeRepository   = null
  var fileContent: Map[String, String] = null

  override def run(request: Array[Byte]): Array[Byte] = {

    typeRepository = Transformer(CodeGeneratorRequest.parseFrom(request)).filterNot {
      case (typeIdentifier, _) => config.output.filter.packages(typeIdentifier.pakkage.p)
    }

    val (umlFormatter, fileExtension) = config.output.format match {

      case OutputFormat.PLANT_UML =>
        (PlantUMLFormatter, "puml")

      case _ =>
        throw new IllegalArgumentException(s"Output format ${config.output.format} is not yet supported.")
    }

    def createFileName(file: String) = file.replaceAll("\\.[^.]*$", "") + "." + fileExtension

    fileContent = config.output.organization match {

      case OutputFileOrganization.SINGLE_FILE => Map(createFileName(config.output.file) -> umlFormatter(typeRepository.values, typeRepository, config))
      case OutputFileOrganization.DIRECT_MAPPING =>
        typeRepository.values.groupBy(_.origin.file).map { case (file, types) => file -> umlFormatter(types, typeRepository, config) }.map {
          case (file, content) =>
            createFileName(file) -> content
        }

      case OutputFileOrganization.FILE_PER_PACKAGE =>
        typeRepository.values.groupBy(_.identifier.pakkage).map { case (pakkage, types) => pakkage -> umlFormatter(types, typeRepository, config) }.map {
          case (pakkage, content) => createFileName(s"${pakkage.p}.package") -> content
        }
    }

    val responseFiles: Iterable[CodeGeneratorResponse.File] = fileContent.map {
      case (file, content) => CodeGeneratorResponse.File.newBuilder().setName(file).setContent(content).build()
    }

    CodeGeneratorResponse.newBuilder().addAllFile(responseFiles.asJava).build().toByteArray
  }
}

object ProtocUMLGenerator {

  val name: String = "uml"

  def apply(config: Config = Configuration()): ProtocUMLGenerator = new ProtocUMLGenerator(config)
}
