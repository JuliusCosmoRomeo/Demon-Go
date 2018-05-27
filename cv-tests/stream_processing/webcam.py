import cv2

from draw_contours import (
    draw_contours,
    hough_lines,
    hough_linesP,
)
from rectangles import (
    RectangleDrawer
)


def send_data(img):
    jpgdata = cv2.imencode('.jpg', img).tobytes()

    return jpgdata


def fetch_stream():
    r = RectangleDrawer()
    cv2.namedWindow('Video')
    # cv2.createTrackbar('Accuracy', 'Video', 100, 100, lambda x: None)
    cam = cv2.VideoCapture(0)
    while True:
        ret_val, img = cam.read()
        img = cv2.flip(img, 1)
        img = hough_linesP(img)
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
