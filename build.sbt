import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

// Scala versions
val scala213 = "2.13.4"
val scala212 = "2.12.13"
val scala211 = "2.11.12"
val scala3 = List("3.0.0-RC2", "3.0.0-RC3")
val scala2 = List(scala213, scala212, scala211)
val allScalaVersions = scala2 ::: scala3

ThisBuild / name := "reactify"
ThisBuild / organization := "com.outr"
ThisBuild / version := "4.0.5-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / crossScalaVersions := allScalaVersions

ThisBuild / publishTo := sonatypePublishTo.value
ThisBuild / sonatypeProfileName := "com.outr"
ThisBuild / publishMavenStyle := true
ThisBuild / licenses := Seq("MIT" -> url("https://github.com/outr/reactify/blob/master/LICENSE"))
ThisBuild / sonatypeProjectHosting := Some(xerial.sbt.Sonatype.GitHubHosting("outr", "reactify", "matt@outr.com"))
ThisBuild / homepage := Some(url("https://github.com/outr/reactify"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/outr/reactify"),
    "scm:git@github.com:outr/reactify.git"
  )
)
ThisBuild / developers := List(
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.", url=url("http://matthicks.com"))
)

val testyVersion: String = "1.0.5"

lazy val reactify = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "reactify",
    test / publishArtifact := false,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++= Seq(
      "com.outr" %%% "testy" % testyVersion % Test
    )
  )
  .jsSettings(
    crossScalaVersions := allScalaVersions,
    Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .jvmSettings(
    crossScalaVersions := allScalaVersions
  )
  .nativeSettings(
    crossScalaVersions := scala2,
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