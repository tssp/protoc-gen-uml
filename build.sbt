enablePlugins(JavaAppPackaging)

scalaVersion := "2.13.10"

name := "protoc-gen-uml"

organization := "io.coding-me"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.thesamet.scalapb"  %% "compilerplugin"           % "0.11.12",
  "com.github.pureconfig" %% "pureconfig"               % "0.17.2" excludeAll (ExclusionRule(organization = "com.typesafe", name = "config")),
  "com.github.os72"       % "protoc-jar"                % "3.11.4",
  "com.typesafe"          % "config"                    % "1.4.2",
  "org.scalatest"         %% "scalatest-flatspec"       % "3.2.14" % "test",
  "org.scalatest"         %% "scalatest-shouldmatchers" % "3.2.14" % "test"
)

ThisBuild / scalafmtConfig := file(".scalafmt")

Compile / mainClass := Some("io.coding.me.protoc.uml.Main")
