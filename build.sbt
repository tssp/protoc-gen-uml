enablePlugins(JavaAppPackaging)

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.12.8", "2.11.12")

name := "protoc-gen-uml"

organization := "io.coding-me"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.7",
  "com.google.protobuf"    % "protobuf-java"  % "3.8.0",
  "com.github.os72"        %  "protoc-jar"     % "3.8.0",
  "com.typesafe"           %  "config"         % "1.3.4",
  "com.github.pureconfig"  %% "pureconfig"     % "0.10.2" excludeAll(ExclusionRule(organization = "com.typesafe", name = "config")),
  "org.scalatest"          %% "scalatest"      % "3.0.7" % "test"
)

scalafmtConfig in ThisBuild := file(".scalafmt")

mainClass in Compile := Some("io.coding.me.protoc.uml.Main")
