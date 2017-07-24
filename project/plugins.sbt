resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

val scalaJSVersion = Option(System.getenv("SCALAJS_VERSION")).getOrElse("0.6.18")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.8")

{
  if (scalaJSVersion.startsWith("0.6."))
    Seq(addSbtPlugin("org.scala-native" % "sbt-scalajs-crossproject" % "0.2.0"))
  else
    Nil
}