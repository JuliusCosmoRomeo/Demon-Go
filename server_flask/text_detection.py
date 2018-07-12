import queue
import sqlite3
import sys
import uuid
from multiprocessing import Queue
from datetime import datetime

import cv2
import numpy as np

from ocr import get_word_from_img

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

    def draw_text_boxes(self, img, rst):
        for t in rst:
            d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                          t['y2'], t['x3'], t['y3']], dtype='int32')
            d = d.reshape(-1, 2)
            cv2.polylines(
                img, [d], isClosed=True,
                color=(255, 255, 0), thickness=1)
        filename = self.save_image(img)

        self.out_queue.put([filename])

    @staticmethod
    def write_data(values):
        conn = sqlite3.connect("collected_data.db")
        c = conn.cursor()
        c.executemany('INSERT INTO data VALUES (?, ?, ?, ?, ?, ?)', values)
        conn.commit()
        conn.close()

    def crop_text(self, img, points):

        def clamp(n, largest):
            return max(0, min(int(n), largest))

        HEIGHT, WIDTH, _ = img.shape

        files = []
        data = []
        for textbox in points:
            min_x = 10000
            max_x = 0
            min_y = 10000
            max_y = 0
            for k, v in textbox.items():

                if k.startswith('x'):
                    min_x = min(min_x, clamp(v, WIDTH))
                    max_x = max(max_x, clamp(v, WIDTH))
                elif k.startswith('y'):
                    min_y = min(min_y, clamp(v, HEIGHT))
                    max_y = max(max_y, clamp(v, HEIGHT))
                else:
                    cropped_img = img[min_y:max_y, min_x:max_x]
                    filename = self.save_image(cropped_img)
                    files.append(filename)
                    img_text, rotation, conf = get_word_from_img(cropped_img)
                    now = datetime.now().isoformat(timespec='milliseconds')

                    # Cols are user_id, filename, text, rotation, time, confidence
                    data.append((1, filename, img_text, int(rotation), now, str(conf)))

        if data:
            self.write_data(data)
        self.out_queue.put(files)

    def detect_text(self):
        prediction_function = get_predictor(self.CHECKPOINT_PATH)

        while True:
            try:
                img = self.in_queue.get(timeout=self.QUEUE_TIMEOUT)
            except queue.Empty:
                continue

            rst = self.expand_text_box(prediction_function(img)['text_lines'])
            if rst:
                self.process(img, rst)
            else:
                self.out_queue.put([])
