from flask import Flask, render_template
from flask_socketio import SocketIO
from pymongo import MongoClient
from kafka import KafkaProducer
import threading
import time
import json

app = Flask(__name__)
socketio = SocketIO(app)

client = MongoClient("mongodb://localhost:27017/")
db = client["tweets_insights_db"]
collection = db["tweets_insights"]

kafka_producer = KafkaProducer(
    bootstrap_servers=["localhost:9092"],
    value_serializer=lambda x: json.dumps(x).encode("utf-8"),
)


@app.route("/")
def index():
    return render_template("index.html")


def send_insights():
    insights = collection.find_one(
        {},
        {
            "_id": 0,
            "avg_retweets": 1,
            "max_retweets": 1,
            "tweets_count": 1,
            "top_users": 1,
            "tweets_distribution": 1,
        },
    )

    if insights is None:
        return

    insights["top_users"] = [
        {"user": user["_1"], "tweets_count": user["_2"]}
        for user in insights["top_users"]
    ]

    insights["tweets_distribution"] = [
        {"date": date["_1"], "tweets_count": date["_2"]}
        for date in insights["tweets_distribution"]
    ]

    socketio.emit("new_insights", insights)


@socketio.on("connect")
def test_connect(auth=None):
    send_insights()


@socketio.on("tweets_filter_query")
def handle_tweets_filter_query(query):
    kafka_producer.send("tweets-filter-query", {"query": query})


def update_insights():
    while True:
        send_insights()
        time.sleep(1)


if __name__ == "__main__":
    threading.Thread(target=update_insights, daemon=True).start()
    socketio.run(app, port=5000)
