import math

import cv2
import numpy as np
from scipy.signal import convolve2d

from transform import transform


def estimate_noise(img):
    # as per https://stackoverflow.com/a/25436112
    img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    H, W = img.shape[:2]

    M = np.array([[1, -2, 1],
                  [-2, 4, -2],
                  [1, -2, 1]])

    convolved = np.absolute(convolve2d(img, M))
    # convolved = (convolved / np.max(convolved) * 255).astype(np.uint8)

    # cv2.imshow('Noise', convolved)

    sigma = np.sum(np.sum(convolved))
    sigma = sigma * math.sqrt(0.5 * math.pi) / (6 * (W-2) * (H-2))

    return sigma


def estimate_lines(img):
    processed = transform(img)
    lines = cv2.HoughLines(processed, 1, np.pi/180, 60)
    # lines = cv2.HoughLinesP(
    #     processed,
    #     rho=1,
    #     theta=np.pi/180,
    #     threshold=100,
    #     minLineLength=20,
    #     maxLineGap=50
    # )
    return len(lines) if lines is not None else 0


def estimate_color_variation(image):
    (B, G, R) = cv2.split(image.astype("float"))
    rg = np.absolute(R - G)
    yb = np.absolute(0.5 * (R + G) - B)

    (rbMean, rbStd) = (np.mean(rg), np.std(rg))
    (ybMean, ybStd) = (np.mean(yb), np.std(yb))

    stdRoot = np.sqrt((rbStd ** 2) + (ybStd ** 2))
    meanRoot = np.sqrt((rbMean ** 2) + (ybMean ** 2))

    return stdRoot + (0.3 * meanRoot)
