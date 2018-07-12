import argparse

import cv2
import imutils
import pytesseract
from PIL import Image
from pytesseract import Output

from analyse_rotation import analyse_rotation


def get_word_from_img(img):
    rotation = -analyse_rotation(img)
    rotated_img = imutils.rotate_bound(img, rotation)

    img_text = pytesseract.image_to_string(rotated_img, lang='eng+deu', boxes=False, config='--psm 8 --oem 1')
    data = pytesseract.image_to_data(rotated_img, lang='eng+deu', config='--psm 8 --oem 1', output_type=Output.DICT)
    print(img_text + ", " + str(max(data['conf'])))

    return img_text, rotation, max(data['conf'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='blah')
    parser.add_argument('filename')
    args = parser.parse_args()

    img = cv2.imread(args.filename)

    Image.open(args.filename).show()
    get_word_from_img(img)
