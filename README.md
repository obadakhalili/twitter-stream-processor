# Twitter Stream Producer

- Install and setup Kafka and Zookeeper following the steps in the article https://hevodata.com/blog/how-to-install-kafka-on-ubuntu/.

  **NOTE**: I installed Kafka's latest version, but everything else remains the same.

- Create a topic with the name `tweets-stream`.

  ```
  ~/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic TutorialTopic
  ```

  **NOTE**: Because I use a different Kafka version, the command looks like this:

  ```
  ~/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic tweets-stream
  ```

- You can run the consumer and listen for incoming messages using the command:

  ```
  ~/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic tweets-stream --from-beginning
  ```

- `cd` into `stream-producer` and run the Python `producer.py` script to start streaming tweets.
