import base64
import datetime
import multiprocessing

import cv2
import numpy as np
import imutils
from flask import Flask, request, render_template, jsonify
from flask_socketio import SocketIO

from text_detection import (
    OCR,
    TextDetection,
)


app = Flask(__name__)
socketio = SocketIO(app)

td = TextDetection(split=False)


def read_image_data(request):
    image_data = base64.b64decode(request.form['image'])

    return imutils.rotate_bound(
        cv2.imdecode(
            np.fromstring(image_data, dtype=np.uint8),
            cv2.IMREAD_COLOR
        ),
        90
    )


@app.route('/', methods=['GET'])
def index():
    return render_template('index.html')


@app.route('/post', methods=['GET', 'POST'])
def get_image():
    img = read_image_data(request)

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


@app.route('/brand', methods=['POST'])
def add_brand():
    brand_key = request.form['brand']

    if brand_key:
        socketio.emit(
            'brand_timestamp',
            {'brand': brand_key, 'time': f'{datetime.datetime.now()}'}
        )

        return 'success'
    else:
        return 'no key'


@app.route('/ocr', methods=['POST'])
def ocr():

    img = read_image_data(request)
    socketio.emit(
        'new image',
        {'path': TextDetection.save_image(img)}
    )

    boxes = td.expand_text_box(td.prediction_function(img)['text_lines'])

    if boxes:
        td.draw_text_boxes(img, boxes)
        socketio.emit(
            'recognized_text',
            {'path': TextDetection.save_image(img)}
        )
        results = OCR.get_text_cells(img, boxes)

        for result in results:
            socketio.emit(
                'ocr_result',
                result
            )

        return jsonify(results)


@app.route('/test_ocr', methods=['GET'])
def test_ocr():
    img = cv2.imread('examples/TEST_IMG_HAMBACHER.jpg')
    boxes = td.expand_text_box(td.prediction_function(img)['text_lines'])

    results = OCR.get_text_cells(img, boxes)

    for result in results:
        socketio.emit(
            'ocr_result',
            result
        )

    return jsonify(results)


@app.route('/detect_text', methods=['POST'])
def detect_text():
    img = read_image_data(request)

    socketio.emit(
        'new image',
        {'path': TextDetection.save_image(img)}
    )

    boxes = td.expand_text_box(td.prediction_function(img)['text_lines'])

    HEIGHT, WIDTH, _ = img.shape

    if boxes:
        min_x, min_y, max_x, max_y = td.text_bounding_rect(
            boxes, HEIGHT, WIDTH)

        rotated_x = float(min_x + int((max_x-min_x) / 2))
        rotated_y = float(min_y + int((max_y-min_y) / 2))

        mid_point = {
            'x': rotated_y,
            'y': WIDTH - rotated_x,
        }

        print('Trying to consume:')
        td.draw_text_boxes(img, boxes)

        socketio.emit(
            'recognized_text',
            {'path': TextDetection.save_image(img)}
        )

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
