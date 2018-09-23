import cv2


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
