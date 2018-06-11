import base64
import multiprocessing
import queue
import uuid

import cv2
import numpy as np
from flask import Flask, request, render_template
from flask_socketio import SocketIO

import sys
sys.path.append("./EAST")

from EAST.run_demo_server import get_predictor

CHECKPOINT_PATH = 'EAST/east_icdar2015_resnet_v1_50_rbox'


app = Flask(__name__)
socketio = SocketIO(app)


def save_image(img):
    filename = f'static/{uuid.uuid4()}.jpg'

    cv2.imwrite(filename, img)
    return filename


frames = multiprocessing.Queue()
results = multiprocessing.Queue()


def draw_text_boxes(img, rst):
    for t in rst['text_lines']:
        d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                      t['y2'], t['x3'], t['y3']], dtype='int32')
        d = d.reshape(-1, 2)
        cv2.polylines(
            img, [d], isClosed=True,
            color=(255, 255, 0), thickness=3)
    return img


def crop_text(img, points):

    files = []
    for textbox in points:
        min_x = 10000
        max_x = 0
        min_y = 10000
        max_y = 0
        for key, value in textbox.items():

            if key.startswith('x'):
                min_x = min(min_x, int(value))
                max_x = max(max_x, int(value))
            elif key.startswith('y'):
                min_y = min(min_y, int(value))
                max_y = max(max_y, int(value))
            else:
                files.append(save_image(img[min_y:max_y, min_x:max_x]))
    results.put(files)


def find_text():
    prediction_function = get_predictor(CHECKPOINT_PATH)

    while True:
        try:
            img = frames.get(timeout=0.05)
            print('Got frame.')
        except queue.Empty:
            continue
        try:
            rst = prediction_function(img)
            if rst['text_lines']:
                draw_text_boxes(img, rst)
                filename = save_image(img)
                # crop_text(img, rst['text_lines'])
                results.put([filename])
        except cv2.error:
            pass


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
    if img is None or img.shape[0] == 0 or img.shape[1] == 0:
        return 'Nope'

    path = save_image(img)
    socketio.emit(
        'new image',
        {'path': path}
    )

    frames.put_nowait(img.copy())

    for filename in results.get():
        socketio.emit(
            'new image',
            {'path': filename}
        )

    return 'gotcha'


@app.route('/test', methods=['GET', 'POST'])
def test():
    # print("POST-Request", request.form['score'])

    img = cv2.imread('static/IMG_2000.jpg', cv2.IMREAD_COLOR)
    frames.put_nowait(img.copy())

    filename = results.get()
    socketio.emit(
        'new image',
        {'path': filename}
    )

    return 'gotcha'


if __name__ == '__main__':
    p = multiprocessing.Process(target=find_text)
    p.start()

    socketio.run(
        app,
        host='0.0.0.0',
        debug=False,
        use_reloader=False,
    )
