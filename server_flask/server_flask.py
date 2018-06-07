import base64
from datetime import datetime

import os
from flask import Flask, request

app = Flask(__name__)


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


@app.route('/post', methods=['GET', 'POST'])
def get_image():
    image_string = request.form['image']
    # print("POST-Request", request.form['score'])
    save_image(image_string)

    return 'gotcha'


if __name__ == '__main__':
    app.run(debug=True)
