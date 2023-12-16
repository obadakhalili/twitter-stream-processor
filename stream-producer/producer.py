import zipfile
import time
import random
from kafka import KafkaProducer
import json

producer = KafkaProducer(
    bootstrap_servers=["localhost:9092"],
    value_serializer=lambda x: json.dumps(x).encode("utf-8"),
)

with zipfile.ZipFile("./data/tweets.csv.zip", "r") as z:
    with z.open(z.namelist()[0]) as f:
        for i, line in enumerate(f, 1):
            line = line.decode("utf-8")

            if i % 1000 != 0:
                line = line.replace("'", '"')
                attribute_details = line.strip().split(",")

                tweet = {
                    "id": attribute_details[1],
                    "date": int(time.time() * 1000),
                    "user": attribute_details[4],
                    "text": attribute_details[5],
                    "retweets": int(random.random() * 10),
                }

                producer.send("tweets-stream", value=tweet)
            else:
                print(f"Sent {i} messages")
                time.sleep(1)

producer.flush()
