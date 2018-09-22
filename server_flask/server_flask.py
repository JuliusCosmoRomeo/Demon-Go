import base64
import multiprocessing

import cv2
import numpy as np
from flask import Flask, request, render_template, jsonify
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
    print("POST-Request", request.form['user_id'])

    img = cv2.imdecode(
        np.fromstring(image_data, dtype=np.uint8),
        cv2.IMREAD_COLOR
    )

    filename = TextDetection.save_image(img)
    socketio.emit(
        'new image',
        {'path': filename}
    )

    td.in_queue.put_nowait((img.copy(), request.form['user_id']))

    for filename in td.out_queue.get():
        socketio.emit(
            'new image',
            {'path': filename}
        )

    return 'gotcha'


@app.route('/detect_text', methods=['POST'])
def detect_text():
    image_data = base64.b64decode(request.form['image'])

    img = cv2.imdecode(
        np.fromstring(image_data, dtype=np.uint8),
        cv2.IMREAD_COLOR
    )

    path = TextDetection.save_image(img)
    socketio.emit(
        'new image',
        {'path': path}
    )

    boxes = td.expand_text_box(td.prediction_function(img)['text_lines'])

    HEIGHT, WIDTH, _ = img.shape

    if boxes:
        min_x, min_y, max_x, max_y = td.text_bounding_rect(
            boxes, HEIGHT, WIDTH)
        mid_point = {
            'x': float(min_x + int((max_x-min_x) / 2)),
            'y': float(min_y + int((max_y-min_y) / 2)),
        }
        print(mid_point)
        return jsonify(mid_point)
    else:
        return jsonify({})


@app.route('/test', methods=['POST', 'GET'])
def test():
    filename = 'examples/fsr.jpg'
    print(filename)
    user_id = '1337'

    img = cv2.imread(filename, cv2.IMREAD_COLOR)
    socketio.emit(
        'new image',
        {'path': filename}
    )

    td.in_queue.put_nowait((img.copy(), user_id))

    for filename in td.out_queue.get():
        socketio.emit(
            'new image',
            {'path': filename}
        )

    return 'gotcha'


if __name__ == '__main__':
    p = multiprocessing.Process(target=td.detection_loop)
    p.start()

    socketio.run(
        app,
        host='0.0.0.0',
        debug=False,
        use_reloader=False,
    )
