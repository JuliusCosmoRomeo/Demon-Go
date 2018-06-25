import sys
import queue
import uuid
from multiprocessing import Queue

import cv2
import numpy as np

from ocr import process_img

sys.path.append("./EAST")
from EAST.run_demo_server import get_predictor


class TextDetection:

    CHECKPOINT_PATH = 'east_icdar2015_resnet_v1_50_rbox'
    QUEUE_TIMEOUT = 0.05

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

    @staticmethod
    def expand_text_box(rst):
        for t in rst:
            t['x0'] -= 5
            t['y0'] -= 5
            t['x1'] += 5
            t['y1'] -= 5
            t['x2'] += 5
            t['y2'] += 5
            t['x3'] -= 5
            t['y3'] += 5
        return rst

    def draw_text_boxes(self, img, rst):
        for t in rst:
            # d = np.array([t['x0'] - 5, t['y0'] - 5, t['x1'] + 5, t['y1'] - 5, t['x2'] + 5,
            #               t['y2'] + 5, t['x3'] - 5, t['y3'] + 5], dtype='int32')
            d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                          t['y2'], t['x3'], t['y3']], dtype='int32')
            d = d.reshape(-1, 2)
            cv2.polylines(
                img, [d], isClosed=True,
                color=(255, 255, 0), thickness=1)
        filename = self.save_image(img)

        self.out_queue.put([filename])

    def crop_text(self, img, points):

        def clamp(n, largest):
            return max(0, min(int(n), largest))

        HEIGHT, WIDTH, _ = img.shape

        files = []
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
                    files.append(self.save_image(cropped_img))
                    process_img(cropped_img)

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
