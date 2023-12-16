name := "SparkApp"
version := "0.1.0"
scalaVersion := "2.12.18"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.1" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.1" % "provided"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.4.1"
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "3.0.1"
