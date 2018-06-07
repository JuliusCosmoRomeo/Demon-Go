import base64
from datetime import datetime

import os
from flask import Flask, request, render_template
from flask_socketio import SocketIO

app = Flask(__name__)
socketio = SocketIO(app)


def save_image(image_string):
    filename = "images/" + datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
    ending = ".jpg"
    suffix = ""

    if os.path.isfile(filename + ending):
        index = 1
        suffix = "(" + str(index) + ")"
        while os.path.isfile(filename + suffix + ending):
            index += 1
            suffix = "(" + str(index) + ")"

    with open(filename + suffix + ending, "wb") as f:
        f.write(base64.b64decode(image_string))

    return filename


@app.route('/', methods=['GET'])
def index():
    return render_template('index.html')


@app.route('/post', methods=['GET', 'POST'])
def get_image():
    image_string = request.form['image']
    # print("POST-Request", request.form['score'])
    filename = save_image(image_string)

    socketio.emit('new image', filename)

    return 'gotcha'


if __name__ == '__main__':
    socketio.run(app, debug=True)
