import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

// Scala versions
val scala213 = "2.13.4"
val scala212 = "2.12.13"
val scala211 = "2.11.12"
val scala3 = "3.0.0-M3"
val allScalaVersions = List(scala213, scala212, scala211, scala3)
val scala2Versions = List(scala213, scala212, scala211)

name in ThisBuild := "reactify"
organization in ThisBuild := "com.outr"
version in ThisBuild := "4.0.4"
scalaVersion in ThisBuild := "2.13.4"
crossScalaVersions in ThisBuild := allScalaVersions

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

val scalatestVersion = "3.2.4-M1"

lazy val reactify = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "reactify",
    publishArtifact in Test := false
  )
  .platformsSettings(JVMPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalatestVersion % Test
    )
  )
  .jsSettings(
    test in Test := {},         // Temporary work-around for ScalaTest not working with Scala.js on Dotty
    libraryDependencies ++= (
      if (isDotty.value) {      // Temporary work-around for ScalaTest not working with Scala.js on Dotty
        Nil
      } else {
        List("org.scalatest" %%% "scalatest" % scalatestVersion % "test")
      }
    )
  )
  .nativeSettings(
    crossScalaVersions := scala2Versions,
  )

//lazy val benchmark = project
//  .in(file("benchmark"))
//  .settings(
//    name := "reactify-benchmark",
//    libraryDependencies ++= Seq(
//      "com.lihaoyi" %% "scalarx" % "0.4.3"
//    ),
//    crossScalaVersions := List(scala213)
//  )
//  .dependsOn(reactifyJVM)