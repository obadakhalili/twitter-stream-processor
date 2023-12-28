import org.apache.log4j.{BasicConfigurator, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

case class TopUser(user: String, tweet_count: Long)

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
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.write.mode("append").parquet(storagePath)

        val accumulatedDF = spark.read.parquet(storagePath)

        val avgRetweets = accumulatedDF.agg(avg($"retweets").as("avg_retweets"))
        val maxRetweets = accumulatedDF.agg(max($"retweets").as("max_retweets"))
        val tweetCount = accumulatedDF.agg(count($"id").as("tweets_count"))
        val topUsers = accumulatedDF.groupBy("user")
          .agg(count("user").alias("tweet_count"))
          .orderBy(desc("tweet_count"))
          .limit(20)
          .as[TopUser]
          .collect()

        val insightsDF = avgRetweets
          .join(maxRetweets)
          .join(tweetCount)
          .withColumn("top_users", typedLit(topUsers))

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
