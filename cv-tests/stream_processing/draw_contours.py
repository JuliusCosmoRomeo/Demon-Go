import cv2
import numpy as np
import imutils

from estimators import estimate_noise
from four_point_transform import four_point_transform

MIN_AREA = 2000


def transform(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    gray = cv2.GaussianBlur(gray, (9, 9), 0)
    high_thresh = cv2.threshold(gray, 70, 255, cv2.THRESH_BINARY)[0]
    low_thresh = 0.5*high_thresh
    gray = cv2.bilateralFilter(gray, 11, 17, 17)
    cv2.imshow('bf', gray)
    gray = cv2.Canny(gray, low_thresh, high_thresh)
    cv2.imshow('adaptive_transform', gray)
    # gray = cv2.Canny(gray, 30, 200)
    # cv2.imshow('transform', gray2)

    return gray


def old_transform(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    thresh = cv2.threshold(blurred, 70, 255, cv2.THRESH_BINARY)[1]

    return thresh


def show_noise(img, approx):
    approx = np.reshape(approx, (4, 2))
    img = img.copy()
    dewarped = four_point_transform(img, approx)
    sigma = estimate_noise(dewarped)
    cv2.imshow('warped', dewarped)

    cv2.putText(
        img,
        f'Sigma: {sigma}',
        (approx[0][0], approx[0][1] + 20),
        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 1
    )


def draw_contours(img):
    processed = transform(img)
    cnts = cv2.findContours(processed.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    cnts = cnts[0] if imutils.is_cv2() else cnts[1]

    for contour in cnts:
        cv2.drawContours(img, contour, -1, (255, 255, 0), 2)
        area = cv2.contourArea(contour)
        if area > MIN_AREA:
            peri = cv2.arcLength(contour, True)
            approx = cv2.approxPolyDP(contour, 0.01 * peri, False)
            # if len(approx) == 4:
                # show_noise(img, approx)
            cv2.drawContours(img, [approx], -1, (0, 255, 0), 2)
            cv2.putText(
                img,
                f'Area: {area}',
                (approx[0][0][0], approx[0][0][1] + 40),
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
