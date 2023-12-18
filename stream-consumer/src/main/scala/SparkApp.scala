import org.apache.log4j.{BasicConfigurator, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

object SparkApp {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(org.apache.log4j.Level.ERROR)
    Logger.getLogger("akka").setLevel(org.apache.log4j.Level.ERROR)
    
    val spark = SparkSession
      .builder()
      .master("local[8]")
      .appName("SparkApp")
      .config("spark.mongodb.output.uri", "mongodb://127.0.0.1/tweets_insights_db")
      .getOrCreate()

    import spark.implicits._

    val kafkaDF = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "tweets-stream")
      .load()
      .selectExpr("CAST(value AS STRING)")
      .select(json_tuple($"value", "id", "date", "user", "text", "retweets"))
      .toDF("id", "date", "user", "text", "retweets")

    val storagePath = "./tweets-storage"

    val reprocessQuery = kafkaDF.writeStream
      .foreachBatch { (batchDF: DataFrame) =>
        batchDF.write.mode("append").parquet(storagePath)

        val accumulatedDF = spark.read.parquet(storagePath)

        val avgRetweets = accumulatedDF.agg(avg($"retweets"))
        val maxRetweets = accumulatedDF.agg(max($"retweets"))
        val tweetCount = accumulatedDF.agg(count($"id"))

        val insightsDF = Seq(
          ("avgRetweets", avgRetweets.toJSON.collect().mkString("[", ",", "]")),
          ("maxRetweets", maxRetweets.toJSON.collect().mkString("[", ",", "]")),
          ("tweetCount", tweetCount.toJSON.collect().mkString("[", ",", "]")),
        ).toDF("type", "data")

        insightsDF.write
          .format("mongo")
          .option("collection", "tweets_insights")
          .mode("overwrite")
          .save()
      }
      .start()

    reprocessQuery.awaitTermination()
  }
}
