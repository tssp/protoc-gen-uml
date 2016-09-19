enablePlugins(JavaAppPackaging)

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.10.5")

name := "protoc-gen-uml"

organization := "io.coding-me"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.41",
  "com.github.os72"        %  "protoc-jar"     % "3.0.0",
  "com.typesafe"           %  "config"         % "1.2.1",
  "com.github.melrief"     %% "pureconfig"     % "0.3.0" excludeAll(ExclusionRule(organization = "com.typesafe", name = "config")),
  "org.scalatest"          %% "scalatest"      % "3.0.0" % "test"
)

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor == 10 =>
      libraryDependencies.value :+ compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
    case _ =>
      libraryDependencies.value :+ "com.github.melrief" %% "pureconfig"  % "0.3.0"
  }
}

scalafmtConfig in ThisBuild := Some(file(".scalafmt"))

mainClass in Compile := Some("io.coding.me.protoc.uml.Main")
