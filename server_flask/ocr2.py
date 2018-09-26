import cv2
import numpy as np
import pytesseract


def apply_threshold(img, argument):
    switcher = {
        # 1: cv2.threshold(cv2.GaussianBlur(img, (9, 9), 0), 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1],
        # 2: cv2.threshold(cv2.GaussianBlur(img, (7, 7), 0), 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1],
        # 3: cv2.threshold(cv2.GaussianBlur(img, (5, 5), 0), 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1],
        # 4: cv2.threshold(cv2.medianBlur(img, 5), 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1],
        # 5: cv2.threshold(cv2.medianBlur(img, 3), 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1],
        # 6: cv2.adaptiveThreshold(cv2.GaussianBlur(img, (5, 5), 0), 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 2),
        # 7: cv2.adaptiveThreshold(cv2.medianBlur(img, 3), 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 31, 2),
        8: cv2.threshold(cv2.bilateralFilter(img, 11, 17, 17), 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)[1],
    }
    return switcher.get(argument, "Invalid method")


def crop_image(img, start_x, start_y, end_x, end_y):
    cropped = img[start_y:end_y, start_x:end_x]
    return cropped


def get_string(img, method=8):

    # img = cv2.resize(img, None, fx=1.2, fy=1.2, interpolation=cv2.INTER_CUBIC)

    method = 8

    # Convert to gray
    img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # Apply dilation and erosion to remove some noise
    kernel = np.ones((1, 1), np.uint8)
    img = cv2.dilate(img, kernel, iterations=1)
    img = cv2.erode(img, kernel, iterations=1)

    cv2.imwrite('processed/pre.jpg', img)

    # for method in range(1, 8):
    #  Apply threshold to get image with only black and white
    proc = apply_threshold(img.copy(), method)
    # proc = transform(img.copy())

    # Recognize text with tesseract for python
    cv2.imwrite(f'processed/method{method}.jpg', proc)
    result = pytesseract.image_to_string(
        proc,
        lang='eng+deu', boxes=False, config='--psm 7 --oem 1'
    )
    # print(f'{method}: {result}')
    return result, proc
