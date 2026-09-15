scalaVersion := "3.9.0"
organization := "cafelox"

libraryDependencies += Dependencies.munit % Test

semanticdbEnabled := true

scalacOptions ++= Seq(
    // format: off
    "-encoding", "UTF-8",
    "-release", "25",
    "-explain",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Wsafe-init",
    "-Wunused:all",
    "-Wvalue-discard",
    "-Yexplicit-nulls",
    // format: on
  )

lazy val cafelox = rootProject.autoAggregate
  .settings(
    publish / skip := true
  )

lazy val runtime = (project in file("runtime"))
  .settings(
    name := "runtime"
  )

lazy val compiler = (project in file("compiler"))
  .dependsOn(runtime)
  .settings(
    name := "compiler"
  )

lazy val cli = (project in file("cli"))
  .dependsOn(compiler)
  .settings(
    name := "cli"
  )
