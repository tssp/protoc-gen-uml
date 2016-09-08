enablePlugins(JavaAppPackaging)

scalaVersion := "2.11.8"

name := "protoc-gen-uml"

organization := "io.coding-me"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.41",
  "com.github.os72"        % "protoc-jar"      % "3.0.0",
  "com.iheart"             %% "ficus"          % "1.2.6",
  "org.scalatest"          %% "scalatest"      % "3.0.0" % "test"
)

scalafmtConfig in ThisBuild := Some(file(".scalafmt"))

mainClass in Compile := Some("io.coding.me.protoc.uml.Main")
