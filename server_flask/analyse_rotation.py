# -*- coding: utf-8 -*-
"""
Automatically detect rotation and line spacing of an image of text using
Radon transform

If image is rotated by the inverse of the output, the lines will be
horizontal (though they may be upside-down depending on the original image)

It doesn't work with black borders
"""

from __future__ import division, print_function

import argparse
import warnings

import cv2
import matplotlib.pyplot as plt
import numpy
from matplotlib.mlab import rms_flat
from numpy import mean, array, blackman
from numpy.fft import rfft
from skimage.transform import radon

try:
    # More accurate peak finding from
    # https://gist.github.com/endolith/255291#file-parabolic-py
    from parabolic import parabolic


    def argmax(x):
        return parabolic(x, numpy.argmax(x))[0]
except ImportError:
    from numpy import argmax


def analyse_rotation(img, plot=False, line_spacing=False):
    # converting to grayscale
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    I = gray - mean(gray)  # Demean; make the brightness extend above and below zero

    # Do the radon transform and display the result
    with warnings.catch_warnings(): # Some warning inside the radon
        warnings.simplefilter("ignore")
        sinogram = radon(I)

    # Find the RMS value of each row and find "busiest" rotation,
    # where the transform is lined up perfectly with the alternating dark
    # text and white lines
    with warnings.catch_warnings(): # rms_flat is deprecated but I didn't found an alternative. Still works though
        warnings.simplefilter("ignore")
        r = array([rms_flat(line) for line in sinogram.transpose()])
    rotation = 90 - argmax(r)

    # Plot the busy row
    row = sinogram[:, rotation]
    N = len(row)

    # Take spectrum of busy row and find line spacing
    window = blackman(N)
    spectrum = rfft(row * window)
    frequency = argmax(abs(spectrum))

    if line_spacing:
        line_spacing = N / frequency  # pixels
        print('Line spacing: {:.2f} pixels'.format(line_spacing))

    if plot:
        print('Rotation: {:.2f} degrees'.format(rotation))
        plt.subplot(2, 2, 1)
        plt.imshow(I)

        plt.subplot(2, 2, 2)
        plt.imshow(sinogram.T, aspect='auto')
        plt.gray()

        plt.axhline(rotation, color='r')

        plt.subplot(2, 2, 3)
        plt.plot(row)

        plt.plot(row * window)

        plt.subplot(2, 2, 4)
        plt.plot(abs(spectrum))
        plt.axvline(frequency, color='r')
        plt.yscale('log')
        plt.show()

    return rotation


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='blah')
    parser.add_argument('filename')
    args = parser.parse_args()

    filename = args.filename
    # img = Image.open(filename)

    img = cv2.imread(filename, cv2.IMREAD_COLOR)

    # analyse_rotation(img, True, True)
    analyse_rotation(img, True, True)
