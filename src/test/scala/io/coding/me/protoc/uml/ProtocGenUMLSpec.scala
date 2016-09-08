package io.coding.me.protoc.uml.model

import io.coding.me.protoc.uml._
import io.coding.me.protoc.uml.config._
import io.coding.me.protoc.uml.model.FieldTypes._
import io.coding.me.protoc.uml.model.Multiplicities._
import io.coding.me.protoc.uml.model.MessageFields._
import io.coding.me.protoc.uml.model.Types._
import io.coding.me.protoc.uml.model._
import org.scalatest.Inside
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import com.github.os72.protocjar.Protoc

import protocbridge._
import java.io.File
import java.nio.file.Files

import scala.collection.JavaConversions._

abstract class ProtocGenUMLSpec(name: String, folder: String) extends FlatSpec with Matchers with Inside {

  private val tmpFile = Files.createTempDirectory("compiled-protos-").toFile
  private var testDir: String          = null
  private var testFiles: Array[String] = Array()

  protected def config: Config = Configuration()

  protected lazy val protocUMLGenerator = ProtocUMLGenerator()

  tmpFile.deleteOnExit

  s"The $name PB model transformer " should "load proper proto files" in {

    // Not the best way to search for files in resource directory,
    // but gets the job done when executing as part of sbt test.

    testDir = Seq(new File(".").getCanonicalPath, "src", "test", "resources", "sample-protos", folder).mkString(File.separator)

    val d = new File(testDir)

    d.exists should be(true)
    d.isDirectory should be(true)

    testFiles = d.listFiles.toArray.filterNot(_.getName.startsWith(".")).map(_.getAbsolutePath)
    testFiles.nonEmpty should be(true)
  }

  it should "compile the protos at all" in {

    val protocArgs = Array(s"--uml_out=$tmpFile", "-I", testDir) ++ testFiles

    val code = ProtocBridge
      .runWithGenerators(a => Protoc.runProtoc(s"-v${ProtocVersion.v3}" +: a.toArray), Seq(ProtocUMLGenerator.name -> protocUMLGenerator), protocArgs)

    code should be(0)
  }

  it should "have a non empty type repository" in {

    protocUMLGenerator.typeRepository should not be (empty)
  }
}

class SimpleProtocGenUMLSpec extends ProtocGenUMLSpec("simple", "p1") {

  lazy val pakkage        = Package("test.package")
  lazy val typeRepository = protocUMLGenerator.typeRepository

  it should "have a simple message type" in {

    val identifier = TypeIdentifier(pakkage, Name("SearchRequest"))

    typeRepository should contain key (identifier)
    typeRepository should not contain key(identifier.copy(name = Name(s"SearchRequest${TYPE_NAME_SEPARATOR}ProjectsEntry")))
    typeRepository should not contain key(identifier.copy(name = Name(s"SearchRequest${TYPE_NAME_SEPARATOR}ProjectsInverseEntry")))

    inside(typeRepository(identifier)) {
      case Types.MessageType(id, enclosingType, fields, origin) =>
        id should be(identifier)
        enclosingType should be(None)
        fields should not be (empty)

        fields should contain(MessageFields.TypedField("query", ScalarValueType("String"), Some(Optional)))
        fields should contain(MessageFields.TypedField("page_number", ScalarValueType("Int"), Some(Optional)))
        fields should contain(MessageFields.TypedField("result_per_page", ScalarValueType("Int"), Some(Optional)))
        fields should contain(MessageFields.TypedField("projects", MapType(ScalarValueType("String"), ScalarValueType("Int")), None))
        fields should contain(MessageFields.TypedField("projects_inverse", MapType(ScalarValueType("Int"), ScalarValueType("String")), None))

    }
  }

  it should "have a simple enum type" in {

    val identifier = TypeIdentifier(pakkage, Name("Corpus"))

    typeRepository should contain key (identifier)
    inside(typeRepository(identifier)) {
      case Types.EnumType(id, enclosingType, values, origin) =>
        id should be(identifier)
        enclosingType should be(None)
        values.map(_.name) should contain only ("UNIVERSAL", "WEB", "VIDEO", "PRODUCTS", "NEWS", "LOCAL", "IMAGES")
    }
  }
}

class OneOfProtocGenUMLSpec extends ProtocGenUMLSpec("oneOf", "p2") {

  lazy val pakkage        = Package("test.package")
  lazy val typeRepository = protocUMLGenerator.typeRepository

  it should "have a simple message type with oneOf field" in {

    val identifier = TypeIdentifier(pakkage, Name("SampleMessage"))

    typeRepository should contain key (identifier)

    typeRepository should contain key (TypeIdentifier(pakkage, Name(s"SampleMessage${TYPE_NAME_SEPARATOR}TestOneof")))
    typeRepository should contain key (TypeIdentifier(pakkage, Name(s"SampleMessage${TYPE_NAME_SEPARATOR}TestOneof2")))
    inside(typeRepository(identifier)) {
      case Types.MessageType(id, enclosingType, fields, origin) =>
        id should be(identifier)
        enclosingType should be(None)
        fields should not be (empty)

        fields should contain(MessageFields.TypedField("other", ScalarValueType("String"), Some(Optional)))
        fields should contain(MessageFields.OneOfField("test_oneof", TypeIdentifier(Package("test.package"), Name(s"SampleMessage${TYPE_NAME_SEPARATOR}TestOneof"))))
        fields should contain(MessageFields.OneOfField("test_oneof2", TypeIdentifier(Package("test.package"), Name(s"SampleMessage${TYPE_NAME_SEPARATOR}TestOneof2"))))
    }
  }
}

class NestedProtocGenUMLSpec extends ProtocGenUMLSpec("nested", "p3") {

  lazy val pakkage        = Package("test.package")
  lazy val typeRepository = protocUMLGenerator.typeRepository

  val identifier             = TypeIdentifier(pakkage, Name("SampleMessage"))
  val nestedIdentifier       = TypeIdentifier(pakkage, Name(s"SampleMessage${TYPE_NAME_SEPARATOR}SubMessage"))
  val nestedNestedIdentifier = TypeIdentifier(pakkage, Name(s"SampleMessage${TYPE_NAME_SEPARATOR}SubMessage${TYPE_NAME_SEPARATOR}SubSubMessage"))
  val containerIdentifier    = TypeIdentifier(pakkage, Name("Container"))

  it should "have nested message types" in {

    typeRepository should contain key (identifier)
    typeRepository should contain key (nestedIdentifier)
    typeRepository should contain key (nestedNestedIdentifier)
    typeRepository should contain key (containerIdentifier)
  }

  it should "have fields based on nested message types" in {

    inside(typeRepository(identifier)) {
      case Types.MessageType(id, enclosingType, fields, origin) =>
        fields should contain(MessageFields.TypedField("sub_message", CompoundType(nestedIdentifier), Some(Optional)))
        fields should contain(MessageFields.TypedField("sub_sub_message", CompoundType(nestedNestedIdentifier), Some(Optional)))
        fields should contain(MessageFields.TypedField("container", CompoundType(containerIdentifier), Some(Optional)))
    }
  }
}

class ComplexProtocGenUMLSpec extends ProtocGenUMLSpec("complex", "complex") {

  lazy val typeRepository = protocUMLGenerator.typeRepository
  it should "have all expected types" in {

    val musicPackage    = Package("io.coding.me.schema.music")
    val utilPackage     = Package("io.coding.me.schema.util")
    val databasePackage = Package("io.coding.me.schema.database")

    typeRepository.keys.map(_.pakkage) should contain (musicPackage)
    typeRepository.keys.map(_.pakkage) should contain (utilPackage)

    List("Date").map(Name).map(n => TypeIdentifier(utilPackage, n)).foreach { typeIdentifier =>

      typeRepository.keys should contain (typeIdentifier)
    }

    List("Album", s"Album${TYPE_NAME_SEPARATOR}Genre", s"Album${TYPE_NAME_SEPARATOR}Interpret", "Musician", "Band").map(Name).map(n => TypeIdentifier(musicPackage, n)).foreach { typeIdentifier =>

      typeRepository.keys should contain (typeIdentifier)
    }

}}
