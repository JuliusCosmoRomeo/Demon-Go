import cv2
import requests

cam = cv2.VideoCapture(0)

ret, frame = cam.read()
data = cv2.imencode('.jpg', frame)[1].tostring()

requests.post('http://127.0.0.1:5555/post', data=data)
