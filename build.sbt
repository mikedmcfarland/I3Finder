import com.github.retronym.SbtOneJar._

oneJarSettings

packAutoSettings

name := "I3Finder"

version := ".9"

scalaVersion := "2.10.4"

mainClass := Some("I3Finder")

resolvers += "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.2.0"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.2.1"
