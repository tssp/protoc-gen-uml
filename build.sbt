enablePlugins(JavaAppPackaging)

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8")

name := "protoc-gen-uml"

organization := "io.coding-me"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.41",
  "com.github.os72"        %  "protoc-jar"     % "3.0.0",
  "com.typesafe"           %  "config"         % "1.3.3",
  "com.github.melrief"     %% "pureconfig"     % "0.3.0" excludeAll(ExclusionRule(organization = "com.typesafe", name = "config")),
  "org.scalatest"          %% "scalatest"      % "3.0.0" % "test"
)

scalafmtConfig in ThisBuild := Some(file(".scalafmt"))

mainClass in Compile := Some("io.coding.me.protoc.uml.Main")
