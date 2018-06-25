import argparse

import cv2
import imutils
import pytesseract
from PIL import Image


def process_img(img):
    # No real difference
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # Trash
    # gray = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)[1]

    # Auch eher unhilfreich
    # gray = cv2.medianBlur(gray, 3)

    # for i in range(-2, 3):
    #     new_angle = angle + i
    #     print("Angle:", new_angle)
    #     rotated = imutils.rotate_bound(gray, new_angle)
    #     cv2.imshow("Rotated (Correct)", rotated)
    #     cv2.waitKey(0)

    # img_text = pytesseract.image_to_string(img, lang="deu")
    img_text = pytesseract.image_to_string(img, lang='eng', boxes=False, config='--psm 8 --oem 1')
    print(img_text)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='blah')
    parser.add_argument('filename')
    args = parser.parse_args()

    img = cv2.imread(args.filename)

    Image.open(args.filename).show()
    process_img(img)
