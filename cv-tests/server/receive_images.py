import cv2
import numpy as np

from flask import (
    Flask,
    request,
    jsonify,
)

app = Flask(__name__)


@app.route('/', methods=['POST'])
def receive_image():
    nparray = np.fromstring(request.data, dtype=np.uint8)
    img = cv2.imdecode(nparray, cv2.IMREAD_COLOR)
    cv2.imwrite('captured.jpg', img)
    # cv2.namedWindow('Frame')
    # while True:
    #     cv2.imshow('Frame', img)
    return jsonify({'status': 200})


app.run(
    host='localhost',
    port=5555,
)
