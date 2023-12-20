# Twitter Stream Producer

- Install and setup Kafka and Zookeeper following the steps in the article https://hevodata.com/blog/how-to-install-kafka-on-ubuntu/.

- Create a topic with the name `tweets-stream`.

  ```
  ~/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic tweets-stream
  ```

- You can run the consumer and listen for incoming messages using the command:

  ```
  ~/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic tweets-stream --from-beginning
  ```

- `cd` into `stream-producer` and run the Python `producer.py` script to start streaming tweets.

- `cd` into `stream-consumer` and run the `./submit-spark-app.sh` script to start the Spark Streaming application. Read the sub-project README's to learn what you need in order to run the project.

- `cd` into `web` and run the web app with the command `python app.py`. Go to `localhost:5000` to see the insights processed by the Spark Streaming application of the tweets produced by the Python script. The insights are updated every 2 seconds.
