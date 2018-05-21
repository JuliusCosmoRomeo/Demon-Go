#!/usr/bin/env python
from flask import Flask, Response

import cv2

app = Flask(__name__)


class Camera:
    def __init__(self):
        self.cap = cv2.VideoCapture(0)

    def get_frame(self):
        ret, frame = self.cap.read()
        data = cv2.imencode('.jpeg', frame)[1].tobytes()
        return data

    def __del__(self):
        self.cap.release()


def gen(camera):
    while True:
        frame = camera.get_frame()
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')


@app.route('/video_feed')
def video_feed():
    return Response(gen(Camera()),
                    mimetype='multipart/x-mixed-replace; boundary=frame')


if __name__ == '__main__':
    app.run(
        host='127.0.0.1',
        debug=True,
        port=5000
    )
