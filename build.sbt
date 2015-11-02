name := """biblio-user-recommender"""

version := "1.0"

scalaVersion := "2.10.6"

resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/"
)

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.5.1" % "provided"
libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.5.1" % "provided"

libraryDependencies += "org.anormcypher" % "anormcypher_2.10" % "0.6.0"

