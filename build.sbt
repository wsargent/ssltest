import play.PlayImport.PlayKeys
import play.PlayScala

name := "ssltest"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  ws
)

// Keys.fork in (Test,run) := true

javaOptions in Test ++= Seq("-Dcom.sun.security.enableCRLDP=true", "-Dcom.sun.net.ssl.checkRevocation=true", "-Djavax.net.debug=off")

lazy val root = (project in file(".")).addPlugins(PlayScala)