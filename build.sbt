import sbtcrossproject.{crossProject, CrossType}

name in ThisBuild := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "2.3.0"
scalaVersion in ThisBuild := "2.12.4"
crossScalaVersions in ThisBuild := List("2.12.4", "2.11.12", "2.13.0-M2")

publishTo in ThisBuild := sonatypePublishTo.value
sonatypeProfileName in ThisBuild := "com.outr"
publishMavenStyle in ThisBuild := true
licenses in ThisBuild := Seq("MIT" -> url("https://github.com/outr/reactify/blob/master/LICENSE"))
sonatypeProjectHosting in ThisBuild := Some(xerial.sbt.Sonatype.GithubHosting("outr", "reactify", "matt@outr.com"))
homepage in ThisBuild := Some(url("https://github.com/outr/reactify"))
scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/outr/reactify"),
    "scm:git@github.com:outr/reactify.git"
  )
)
developers in ThisBuild := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.", url=url("http://matthicks.com"))
)

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
    name := "reactify-tests",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js