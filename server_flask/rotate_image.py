import imutils as imutils
import cv2

angle = -5
image = cv2.imread("test5.jpg")

rotated = imutils.rotate_bound(image, angle)
cv2.imshow("Rotated (Correct)", rotated)
cv2.waitKey(0)