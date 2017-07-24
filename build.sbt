name := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "2.0.6"
scalaVersion in ThisBuild := "2.12.2"
crossScalaVersions in ThisBuild := List("2.12.2", "2.11.11", "2.13.0-M1")

lazy val root = project.in(file("."))
  .aggregate(js, jvm)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val reactify = crossProject.in(file("."))
  .settings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.3" % "test"
  )
lazy val js = reactify.js
lazy val jvm = reactify.jvm