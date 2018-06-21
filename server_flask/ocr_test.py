import cv2
import imutils
import pytesseract
from PIL import Image

####### Code for rotation with different preprocessing - most of the times just not good
angle = -5
img = cv2.imread("test_small_5.png")
cv2.imshow("Rotated (Correct)", img)
cv2.waitKey(0)

# No real difference
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Trash
# gray = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)[1]

# Auch eher unhilfreich
# gray = cv2.medianBlur(gray, 3)

for i in range(-2, 3):
    new_angle = angle + i
    print("Angle:", new_angle)
    rotated = imutils.rotate_bound(gray, new_angle)
    cv2.imshow("Rotated (Correct)", rotated)
    cv2.waitKey(0)

    img_text = pytesseract.image_to_string(rotated, lang="deu")
    print("Text:", img_text)


