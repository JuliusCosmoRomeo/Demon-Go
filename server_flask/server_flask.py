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


def draw_text_boxes(illu, rst):
    for t in rst['text_lines']:
        d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                      t['y2'], t['x3'], t['y3']], dtype='int32')
        d = d.reshape(-1, 2)
        cv2.polylines(
            illu, [d], isClosed=True,
            color=(255, 255, 0), thickness=1)
    return illu


def find_text():
    prediction_function = get_predictor(CHECKPOINT_PATH)

    while True:
        try:
            img = frames.get(timeout=0.5)
            print('Got frame.')
        except queue.Empty:
            continue
        print('Processing image..')
        rst = prediction_function(img)
        draw_text_boxes(img, rst)
        print(f'Found {len(rst)} results')
        filename = save_image(img)
        results.put(rst)
        socketio.emit(
            'new image',
            {'path': filename}
        )


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

    frames.put_nowait(img.copy())

    return 'gotcha'


if __name__ == '__main__':
    p = multiprocessing.Process(target=find_text)
    p.start()

    socketio.run(
        app,
        host='0.0.0.0',
        debug=True,
        use_reloader=False,
    )
