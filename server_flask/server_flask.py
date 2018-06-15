import base64
import multiprocessing

import cv2
import numpy as np
from flask import Flask, request, render_template
from flask_socketio import SocketIO

from text_detection import TextDetection

app = Flask(__name__)
socketio = SocketIO(app)

td = TextDetection(split=False)


@app.route('/', methods=['GET'])
def index():
    return render_template('index.html')


@app.route('/post', methods=['GET', 'POST'])
def get_image():
    image_data = base64.b64decode(request.form['image'])
    # print("POST-Request", request.form['score'])

    img = cv2.imdecode(
        np.fromstring(image_data, dtype=np.uint8),
        cv2.IMREAD_COLOR
    )

    path = TextDetection.save_image(img)
    socketio.emit(
        'new image',
        {'path': path}
    )

    td.in_queue.put_nowait(img.copy())

    for filename in td.out_queue.get():
        socketio.emit(
            'new image',
            {'path': filename}
        )

    return 'gotcha'


@app.route('/test', methods=['GET', 'POST'])
def test():
    # print("POST-Request", request.form['score'])

    img = cv2.imread('static/TEST_IMAGE2.jpg', cv2.IMREAD_COLOR)
    td.in_queue.put_nowait(img.copy())

    for filename in td.out_queue.get():
        socketio.emit(
            'new image',
            {'path': filename}
        )

    return 'gotcha'


if __name__ == '__main__':
    p = multiprocessing.Process(target=td.detect_text)
    p.start()

    socketio.run(
        app,
        host='0.0.0.0',
        debug=False,
        use_reloader=False,
    )
