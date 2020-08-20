import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name in ThisBuild := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "4.0.1-SNAPSHOT"
scalaVersion in ThisBuild := "2.13.3"
crossScalaVersions in ThisBuild := List("2.13.3", "2.12.12", "2.11.12")

publishTo in ThisBuild := sonatypePublishTo.value
sonatypeProfileName in ThisBuild := "com.outr"
publishMavenStyle in ThisBuild := true
licenses in ThisBuild := Seq("MIT" -> url("https://github.com/outr/reactify/blob/master/LICENSE"))
sonatypeProjectHosting in ThisBuild := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "reactify", "matt@outr.com"))
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

val scalatestVersion = "3.2.2-M2"

lazy val reactify = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "reactify",
    publishArtifact in Test := false,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalatestVersion % "test"
    )
  )
  .nativeSettings(
    nativeLinkStubs := true,
    scalaVersion := "2.11.12",
    crossScalaVersions := List("2.11.12")
  )
  .jvmSettings(
    crossScalaVersions := List("2.13.3", "0.26.0-RC1", "2.12.12", "2.11.12")
  )