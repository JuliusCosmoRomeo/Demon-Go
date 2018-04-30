import cv2
import numpy as np
import imutils

from estimators import estimate_noise
from four_point_transform import four_point_transform

def draw_contours(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    VALUE = cv2.getTrackbarPos('thrs1', 'Video')
    thresh = cv2.threshold(blurred, VALUE, 255, cv2.THRESH_BINARY)[1]
    
    cnts = cv2.findContours(thresh.copy(), cv2.RETR_EXTERNAL,
        cv2.CHAIN_APPROX_SIMPLE)
    cnts = cnts[0] if imutils.is_cv2() else cnts[1]
    AREA = cv2.getTrackbarPos('AREA', 'Video')

    for c in cnts:
        if cv2.contourArea(c) > AREA :
            peri = cv2.arcLength(c, True)
            approx = cv2.approxPolyDP(c, 0.04 * peri, True)
            if len(approx) == 4:
                approx = np.reshape(approx, (4,2))
                dewarped = four_point_transform(img, approx)
                sigma = estimate_noise(dewarped)
                cv2.imshow('warped', dewarped)
                cv2.putText(
                    img,
                    str(sigma),
                    (approx[0][0], approx[0][1] + 20),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 1
                )
                cv2.drawContours(img, [approx], -1, (0, 255, 0), 2)
