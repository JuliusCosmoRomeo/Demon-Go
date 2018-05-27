import math

import cv2
import numpy as np
from scipy.signal import convolve2d


def estimate_noise(img):
    # as per https://stackoverflow.com/a/25436112
    img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    H, W = img.shape[:2]

    M = np.array([[1, -2, 1],
                  [-2, 4, -2],
                  [1, -2, 1]])

    sigma = np.sum(np.sum(np.absolute(convolve2d(img, M))))
    sigma = sigma * math.sqrt(0.5 * math.pi) / (6 * (W-2) * (H-2))

    return sigma
