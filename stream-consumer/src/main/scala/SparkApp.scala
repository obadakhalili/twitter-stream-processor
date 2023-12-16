import org.apache.log4j.{BasicConfigurator, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

object SparkApp {

  def main(args: Array[String]): Unit = {

    // Configure logger to suppress unnecessary logging
    Logger.getLogger("org").setLevel(org.apache.log4j.Level.ERROR)
    Logger.getLogger("akka").setLevel(org.apache.log4j.Level.ERROR)
    
    // Create Spark session
    val spark = SparkSession
      .builder()
      .master("local[8]")
      .appName("SparkApp")
      .config("spark.mongodb.output.uri", "mongodb://127.0.0.1/tweets_insights_db.tweets_insights")
      .getOrCreate()

    import spark.implicits._

    // Define the DataFrame for reading from Kafka
    val kafkaDF = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "tweets-stream")
      .load()
      .selectExpr("CAST(value AS STRING)")
      .select(json_tuple($"value", "id", "date", "user", "text", "retweets"))
      .toDF("id", "date", "user", "text", "retweets")

    // Process and write insights for each micro-batch
    val query = kafkaDF.writeStream
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        // Example insights from the batch
        val tweetsByUser = batchDF.groupBy("user").count()
        val avgRetweets = batchDF.agg(avg($"retweets"))
        val recentTweets = batchDF.orderBy($"date".desc).limit(10)

        // Function to write insights to MongoDB
        def writeToMongoDB(df: DataFrame, collection: String): Unit = {
          df.write
            .format("mongo")
            .option("collection", collection)
            .mode("append")
            .save()
        }

        // Write each insight to a separate collection in MongoDB
        writeToMongoDB(tweetsByUser, "tweetsByUser")
        writeToMongoDB(avgRetweets, "avgRetweets")
        writeToMongoDB(recentTweets, "recentTweets")
      }
      .start()

    query.awaitTermination()
  }
}
