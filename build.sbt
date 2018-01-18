import sbtcrossproject.{crossProject, CrossType}

name in ThisBuild := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "2.2.0"
scalaVersion in ThisBuild := "2.12.4"
crossScalaVersions in ThisBuild := List("2.12.4", "2.11.12", "2.13.0-M2")

lazy val reactify = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    name := "reactify",
    publishArtifact in Test := false
  )
  .nativeSettings(
    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12")
  )

lazy val reactifyJS = reactify.js
lazy val reactifyJVM = reactify.jvm
lazy val reactifyNative = reactify.native

lazy val tests = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure)
  .dependsOn(reactify)
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    publish := (),
    publishLocal := (),
    publishArtifact := false
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js