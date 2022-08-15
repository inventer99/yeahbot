ThisBuild / version := "0.1.0-SCALA"
ThisBuild / organization := "yeahbot"

ThisBuild / scalaVersion := "3.1.3"

assembly / assemblyMergeStrategy := {
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
  case PathList("META-INF", "gradle", "incremental.annotation.processors") => MergeStrategy.filterDistinctLines
  case x => (assembly / assemblyMergeStrategy).value(x)
}

lazy val root = (project in file("."))
  .settings(
    name := "yeahbot",
    libraryDependencies ++= Seq(
      "org.immutables" % "value" % "2.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "com.discord4j" % "discord4j-core" % "3.2.2"
    ),
    assembly / mainClass := Some("yeahbot.Main")
  )
