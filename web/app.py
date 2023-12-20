from flask import Flask, render_template
from flask_socketio import SocketIO
from pymongo import MongoClient
import threading
import time

app = Flask(__name__)
socketio = SocketIO(app)

client = MongoClient("mongodb://localhost:27017/")
db = client["tweets_insights_db"]
collection = db["tweets_insights"]


@app.route("/")
def index():
    return render_template("index.html")


def send_insights():
    insights = collection.find_one(
        {}, {"_id": 0, "avg_retweets": 1, "max_retweets": 1, "tweets_count": 1}
    )
    socketio.emit("new_insights", insights)


@socketio.on("connect")
def test_connect(auth=None):
    send_insights()


def update_insights():
    while True:
        send_insights()
        time.sleep(1)


if __name__ == "__main__":
    threading.Thread(target=update_insights, daemon=True).start()
    socketio.run(app, port=5000)
