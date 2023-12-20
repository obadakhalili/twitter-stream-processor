#!/bin/sh

echo "Compiling and assembling application..."
sbt assembly

SPARK_HOME=/home/${USER}/dev/spark-bin-dist

# JAR containing a simple hello world
JARFILE=`pwd`/target/scala-2.12/SparkApp-assembly-0.1.0.jar

rm -rf tweets-storage
mkdir tweets-storage

# Run it locally
${SPARK_HOME}/bin/spark-submit --class SparkApp --master local $JARFILE
