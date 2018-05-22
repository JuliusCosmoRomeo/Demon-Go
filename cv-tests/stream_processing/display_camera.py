
"""
Display the content of the webcam stream while running text detection
in an additional process that returns coordinates of recognized text fields.
"""

import cv2
import numpy as np
import imutils

from time import sleep
from urllib.request import urlopen
from urllib.error import URLError

from draw_contours import (
    draw_contours,
    hough_lines,
)
from rectangles import (
    RectangleDrawer
)

url = 'http://172.18.1.198:8080'

cam = cv2.VideoCapture(0)


def fetch_stream():
    r = RectangleDrawer()
    cv2.namedWindow('Video')

    # cv2.createTrackbar('AREA', 'Video', 7000, 100000, lambda x: None)
    # cv2.createTrackbar('thrs1', 'Video', 17, 50, lambda x: None)
    # cv2.createTrackbar('Gauss', 'Video', 11, 50, lambda x: None)

    while True:
        try:
            stream = urlopen(url)
            break
        except URLError:
            print('Stream could not be opened, retrying in 3')
            sleep(3)
    data = b''
    while True:
        data += stream.read(10240)
        a = data.find(b'\xff\xd8')
        b = data.find(b'\xff\xd9')
        if a != -1 and b != -1:
            jpg = data[a:b+2]
            data = data[b+2:]
            img = imutils.rotate_bound(
                cv2.imdecode(
                    np.fromstring(jpg, dtype=np.uint8),
                    cv2.IMREAD_COLOR),
                90
            )
            draw_contours(img)
            cv2.setMouseCallback('Video', r.on_mouse, 0)
            r.draw(img)
            cv2.imshow('Video', img)
            key = cv2.waitKey(1)
            if key == 27:
                exit(0)
            if key == 114:
                r.reset()


def main():
    fetch_stream()


if __name__ == '__main__':
    main()
