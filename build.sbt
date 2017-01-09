name := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.3.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := List("2.12.1", "2.11.8")
sbtVersion in ThisBuild := "0.13.13"
resolvers in ThisBuild += Resolver.sonatypeRepo("releases")
resolvers in ThisBuild += "Artima Maven Repository" at "http://repo.artima.com/releases"
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")
libraryDependencies in ThisBuild += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies in ThisBuild += "org.scalactic" %%% "scalactic" % "3.0.1"
libraryDependencies in ThisBuild += "org.scalatest" %%% "scalatest" % "3.0.1" % "test"

lazy val core = crossProject.in(file("."))
  .settings(
    name := "reactify"
  )

lazy val js = core.js
lazy val jvm = core.jvm