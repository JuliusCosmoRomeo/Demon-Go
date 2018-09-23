import cv2
import numpy as np
import imutils

from estimators import (
    estimate_noise,
    estimate_color_variation,
)
from four_point_transform import four_point_transform
from transform import transform

MIN_AREA = 500


def show_noise(img, approx=None):
    approx = np.reshape(approx, (4, 2))
    dewarped = four_point_transform(img, approx)
    sigma = estimate_noise(dewarped)
    color = estimate_color_variation(dewarped)
    cv2.imshow('warped', dewarped)

    cv2.putText(
        img,
        f'Sigma: {sigma:.2f} Color: {color:.2f}',
        (approx[0][0], approx[0][1] - 10),
        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 1
    )
    return sigma, color


def draw_contours(img):
    processed = transform(img)
    cnts = cv2.findContours(processed.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    cnts = cnts[0] if imutils.is_cv2() else cnts[1]

    for contour in cnts:
        area = cv2.contourArea(contour)
        if area > MIN_AREA:
            peri = cv2.arcLength(contour, True)
            approx = cv2.approxPolyDP(contour, 0.01 * peri, True)
            cv2.drawContours(img, contour, -1, (255, 255, 0), 2)
            if len(approx) == 4:
                sigma, color = show_noise(img, approx)
            # else:
            #     rect = cv2.minAreaRect(contour)
            #     box = cv2.boxPoints(rect)
            #     box = np.int0(box)
            #     sigma, color = show_noise(img, box)

                if sigma > 1.0 and color < 90:
                    fill = (0, 255, 0)
                else:
                    fill = (255, 0, 0)
                cv2.drawContours(img, [approx], -1, fill, 2)
                cv2.putText(
                    img,
                    f'A: {area}',
                    (approx[0][0][0], approx[0][0][1] + 10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 1
                )

    return img


def hough_lines(img):
    processed = transform(img)
    lines = cv2.HoughLines(processed, 1, np.pi/180, 60)
    if lines is not None:
        for line in lines:
            rho, theta = line[0]
            a = np.cos(theta)
            b = np.sin(theta)
            x0 = a*rho
            y0 = b*rho
            x1 = int(x0 + 1000*(-b))
            y1 = int(y0 + 1000*(a))
            x2 = int(x0 - 1000*(-b))
            y2 = int(y0 - 1000*(a))

            cv2.line(img, (x1, y1), (x2, y2), (0, 0, 255), 2)
    return img


def hough_linesP(img):
    processed = transform(img)
    lines = cv2.HoughLinesP(
        processed,
        rho=1,
        theta=np.pi/180,
        threshold=100,
        minLineLength=20,
        maxLineGap=50
    )
    if lines is not None:
        for line in lines:
            x1, y1, x2, y2 = line[0]
            cv2.line(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
    return img
