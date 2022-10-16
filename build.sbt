import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

// Scala versions
val scala213 = "2.13.10"
val scala212 = "2.12.15"
val scala211 = "2.11.12"
val scala3 = List("3.1.1")
val scala2 = List(scala213, scala212, scala211)
val allScalaVersions = scala2 ::: scala3

ThisBuild / name := "reactify"
ThisBuild / organization := "com.outr"
ThisBuild / version := "4.0.8"
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := allScalaVersions

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
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
  Developer(id="darkfrog", name="Matt Hicks", email="matt@matthicks.com", url=url("http://matthicks.com"))
)

val scalaTestVersion: String = "3.2.10"

lazy val reactify = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "reactify",
    test / publishArtifact := false,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.11" % "test"
    ),
    crossScalaVersions := allScalaVersions
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