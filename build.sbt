import sbt.*
import sbt.Keys.*

ThisBuild / scalaVersion := "3.6.1"

lazy val prewaveTask = (project in file("."))
  .settings(
    name := "prewave-task",

    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.11.0",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "com.softwaremill.sttp.client3" %% "core" % "3.10.1",
      "com.softwaremill.sttp.client3" %% "fs2" % "3.10.1",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "com.typesafe" % "config" % "1.4.3",
    )
  )
