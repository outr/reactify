name := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.4.1-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := List("2.12.1", "2.11.8")
sbtVersion in ThisBuild := "0.13.13"

lazy val root = project.in(file("."))
  .aggregate(js, jvm)
  .settings(
    publish := {},
    publishLocal := {},
    test := {}
  )

lazy val reactify = crossProject.crossType(CrossType.Pure).in(file("."))
  .settings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % "test"
  )

lazy val js = reactify.js
lazy val jvm = reactify.jvm