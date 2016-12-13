name := "props"
organization in ThisBuild := "com.outr"
version in ThisBuild := "1.1.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.1"
crossScalaVersions in ThisBuild := List("2.12.1", "2.11.8")
sbtVersion in ThisBuild := "0.13.13"
resolvers in ThisBuild += Resolver.sonatypeRepo("releases")
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

lazy val root = crossProject.in(file("."))
  .settings(
    name := "props",
    resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    libraryDependencies += "org.scalactic" %%% "scalactic" % "3.0.0",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0" % "test"
  )

lazy val js = root.js
lazy val jvm = root.jvm
