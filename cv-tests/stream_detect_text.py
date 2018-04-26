#!/usr/bin/env python3

import sys
sys.path.append("./EAST")

import os

import time
import datetime
import cv2
import numpy as np
import uuid
import json

import functools
import logging
import collections
import tensorflow as tf
import model as model
from icdar import restore_rectangle
import lanms as lanms
from eval import resize_image, sort_poly, detect

CHECKPOINT_PATH = 'EAST/east_icdar2015_resnet_v1_50_rbox'

def get_predictor():

    input_images = tf.placeholder(tf.float32, shape=[None, None, None, 3], name='input_images')
    global_step = tf.get_variable('global_step', [], initializer=tf.constant_initializer(0), trainable=False)

    f_score, f_geometry = model.model(input_images, is_training=False)

    variable_averages = tf.train.ExponentialMovingAverage(0.997, global_step)
    saver = tf.train.Saver(variable_averages.variables_to_restore())

    sess = tf.Session(config=tf.ConfigProto(allow_soft_placement=True))

    ckpt_state = tf.train.get_checkpoint_state(CHECKPOINT_PATH)
    model_path = os.path.join(CHECKPOINT_PATH, os.path.basename(ckpt_state.model_checkpoint_path))
    saver.restore(sess, model_path)

    def predictor(img):
        """
        :return: {
            'text_lines': [
                {
                    'score': ,
                    'x0': ,
                    'y0': ,
                    'x1': ,
                    ...
                    'y3': ,
                }
            ],
            'rtparams': {  # runtime parameters
                'image_size': ,
                'working_size': ,
            },
            'timing': {
                'net': ,
                'restore': ,
                'nms': ,
                'cpuinfo': ,
                'meminfo': ,
                'uptime': ,
            }
        }
        """
        start_time = time.time()
        rtparams = collections.OrderedDict()
        rtparams['start_time'] = datetime.datetime.now()
        rtparams['image_size'] = '{}x{}'.format(img.shape[1], img.shape[0])
        timer = collections.OrderedDict([
            ('net', 0),
            ('restore', 0),
            ('nms', 0)
        ])

        im_resized, (ratio_h, ratio_w) = resize_image(img)
        rtparams['working_size'] = '{}x{}'.format(
            im_resized.shape[1], im_resized.shape[0])
        start = time.time()
        score, geometry = sess.run(
            [f_score, f_geometry],
            feed_dict={input_images: [im_resized[:,:,::-1]]})
        timer['net'] = time.time() - start

        boxes, timer = detect(score_map=score, geo_map=geometry, timer=timer)

        if boxes is not None:
            scores = boxes[:,8].reshape(-1)
            boxes = boxes[:, :8].reshape((-1, 4, 2))
            boxes[:, :, 0] /= ratio_w
            boxes[:, :, 1] /= ratio_h

        duration = time.time() - start_time
        timer['overall'] = duration

        text_lines = []
        if boxes is not None:
            text_lines = []
            for box, score in zip(boxes, scores):
                box = sort_poly(box.astype(np.int32))
                if np.linalg.norm(box[0] - box[1]) < 5 or np.linalg.norm(box[3]-box[0]) < 5:
                    continue
                tl = collections.OrderedDict(zip(
                    ['x0', 'y0', 'x1', 'y1', 'x2', 'y2', 'x3', 'y3'],
                    map(float, box.flatten())))
                tl['score'] = float(score)
                text_lines.append(tl)
        ret = {
            'text_lines': text_lines,
            'rtparams': rtparams,
            'timing': timer,
        }
        return ret


    return predictor

"""
Display the content of the webcam stream while running text detection
in an additional process that returns coordinates of recognized text fields.
"""

import cv2
import multiprocessing
import numpy as np
import queue

from PIL import Image
from time import sleep
from urllib.request import urlopen
from urllib.error import URLError

url = 'http://172.18.1.198:8080/'
DELETE_AFTER = 15

frames = multiprocessing.Queue(1)
results = multiprocessing.Queue()

def find_text():
    prediction_function = get_predictor()

    while True:
        try:
            img = frames.get(timeout=0.05)
        except queue.Empty:
            continue
        rst = prediction_function(img)
        results.put(rst)

class Rotator:
    def __init__(self):
        self._center = 0

    def __call__(self, img):
        h, w = img.shape[:2]
        center = (w / 2, h / 2)
        M = cv2.getRotationMatrix2D(center, 270, 1.0)
        return cv2.warpAffine(img, M, (h, w))

def draw_illu(illu, rst, age):
    for t in rst['text_lines']:
        d = np.array([t['x0'], t['y0'], t['x1'], t['y1'], t['x2'],
                      t['y2'], t['x3'], t['y3']], dtype='int32')
        d = d.reshape(-1, 2)
        cv2.polylines(illu, [d], isClosed=True, color=(255, 255, 0), thickness=int(5 * (1 - age/DELETE_AFTER)))
    return illu

def fetch_stream():
    shapes = []
    rotator = Rotator()

    p = multiprocessing.Process(target=find_text)
    p.start()
    start = True

    while True:
        try:
            stream = urlopen(url)
            break
        except URLError:
            print('Stream could not be opened, retrying...')
    data = b''
    while True:
        data += stream.read(1024)
        a = data.find(b'\xff\xd8')
        b = data.find(b'\xff\xd9')
        if a != -1 and b != -1:
            jpg = data[a:b+2]
            data = data[b+2:]
            img = rotator(cv2.imdecode(np.fromstring(jpg, dtype=np.uint8), cv2.IMREAD_COLOR))
            
            try:
                new_result = results.get_nowait()
            except queue.Empty:
                new_result = False
            
            try:
                if start:
                    start = False
                    frames.put_nowait(img.copy())
                if new_result:
                    shapes.append(new_result)
                    frames.put_nowait(img.copy())
                    new_result = False
            except queue.Full:
                pass

            new_shapes = []
            for result in shapes:
                age = (datetime.datetime.now() - result['rtparams']['start_time']).seconds
                if age < DELETE_AFTER:
                    draw_illu(img, result, age)
                    new_shapes.append(result)
            shapes = new_shapes

            cv2.imshow('Video', img)
            if cv2.waitKey(1) == 27:
                p.join()
                exit(0)

def main():
    fetch_stream()

if __name__ == '__main__':
    main()
