import queue
import sqlite3
import sys
import uuid
from multiprocessing import Queue
from datetime import datetime

import cv2
import imutils
import numpy as np

from ocr import get_word_from_img
from ocr2 import get_string

sys.path.append("./EAST")
from EAST.run_demo_server import get_predictor


class TextDetection:

    CHECKPOINT_PATH = 'east_icdar2015_resnet_v1_50_rbox'
    QUEUE_TIMEOUT = 0.05

    RECTANGLE_EXPANSION_SIZE = 5
    RECTANGLE_EXPANSION = {
        'x0': -1,
        'y0': -1,
        'x1': +1,
        'y1': -1,
        'x2': +1,
        'y2': +1,
        'x3': -1,
        'y3': +1,
    }

    def __init__(self, in_queue=None, out_queue=None, split=False):
        self.in_queue = in_queue or Queue()
        self.out_queue = out_queue or Queue()
        self.prediction_function = get_predictor(self.CHECKPOINT_PATH)

        self.process = self.crop_text if split else self.draw_text_boxes

    @staticmethod
    def save_image(img):
        filename = f'static/{uuid.uuid4()}.jpg'
        print(filename + ": ", end="")

        cv2.imwrite(filename, img)
        return filename

    @classmethod
    def expand_text_box(cls, rst):
        for t in rst:
            for key, value in cls.RECTANGLE_EXPANSION.items():
                t[key] += value * cls.RECTANGLE_EXPANSION_SIZE
        return rst

    @staticmethod
    def clamp(n, upper_bound):
        return max(0, min(int(n), upper_bound))

    @staticmethod
    def text_bounding_rect(textboxes, height, width):

        min_x = 10000
        max_x = 0
        min_y = 10000
        max_y = 0

        for textbox in textboxes:
            for k, v in textbox.items():

                if k.startswith('x'):
                    min_x = min(min_x, TextDetection.clamp(v, width))
                    max_x = max(max_x, TextDetection.clamp(v, width))
                elif k.startswith('y'):
                    min_y = min(min_y, TextDetection.clamp(v, height))
                    max_y = max(max_y, TextDetection.clamp(v, height))

        return (min_x, min_y, max_x, max_y)

    def draw_text_boxes(self, img, rst):
        for t in rst:
            d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                          t['y2'], t['x3'], t['y3']], dtype='int32')
            d = d.reshape(-1, 2)
            rect = cv2.minAreaRect(d)
            box = cv2.boxPoints(rect)
            box = np.int0(box)
            cv2.drawContours(img, [box], 0, (0, 0, 255), 2)

            cv2.polylines(
                img, [d], isClosed=True,
                color=(255, 255, 0), thickness=1)

        return img

    def crop_text(self, img, points, user_id):

        HEIGHT, WIDTH, _ = img.shape
        min_x, min_y, max_x, max_y = self.text_bounding_rect(points)

        cropped_img = img[min_y:max_y, min_x:max_x]
        return cropped_img

    def detection_loop(self):

        while True:
            try:
                img, user_id = self.in_queue.get(timeout=self.QUEUE_TIMEOUT)
            except queue.Empty:
                continue

            rst = self.expand_text_box(
                self.prediction_function(img)['text_lines'])
            if rst:

                modified_img = self.process(img, rst)
                filename = self.save_image(modified_img)

                self.out_queue.put([filename])
            else:
                self.out_queue.put([])


class OCR:

    @staticmethod
    def rotated_subimage(image, center, width, height, theta):

        shape = image.shape[:2]

        matrix = cv2.getRotationMatrix2D(center=center, angle=theta, scale=1)
        image = cv2.warpAffine(src=image, M=matrix, dsize=shape)

        x = int(center[0] - width/2)
        y = int(center[1] - height/2)

        image = image[y:y+int(height), x:int(x+width)]

        return image

    @staticmethod
    def get_text_cells(img, text_boxes):

        results = []

        if text_boxes:

            rectangles = []
            for t in text_boxes:
                d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                              t['y2'], t['x3'], t['y3']], dtype='int32')
                d = d.reshape(-1, 2)
                rect = cv2.minAreaRect(d)
                rectangles.append(rect)

            #     box = cv2.boxPoints(rect)
            #     box = np.int0(box)
            #     rectangles.append(box)
            # rectangles = np.array(rectangles)
            # print(rectangles)
            # results = cv2.groupRectangles(np.concatenate((rectangles, rectangles)), 1, eps=0.05)[0]

            for rect in rectangles:
                try:
                    sub_img = OCR.rotated_subimage(img, rect[0], *rect[1], rect[2])
                    HEIGHT, WIDTH, _ = sub_img.shape
                    if HEIGHT > WIDTH:
                        sub_img = imutils.rotate_bound(
                            sub_img,
                            270
                        )
                    text, processed_img = get_string(sub_img)

                    results.append({
                        'path': TextDetection.save_image(processed_img),
                        'text': text
                    })
                except AttributeError:
                    pass
        return results

    @staticmethod
    def write_data(values):
        conn = sqlite3.connect("collected_data.db")
        c = conn.cursor()
        c.executemany('INSERT INTO data VALUES (?, ?, ?, ?, ?, ?)', values)
        conn.commit()
        conn.close()

    @staticmethod
    def text_from_image(img, filename):
        img_text, rotation, conf = get_word_from_img(img)
        now = datetime.now().isoformat(timespec='milliseconds')

        # Cols are user_id, filename, text, rotation, time, confidence
        return 1, filename, img_text, int(rotation), now, str(conf)
