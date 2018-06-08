import pytesseract
from PIL import Image

with Image.open("test.jpg") as img:
    img_text = pytesseract.image_to_data(img, lang="deu")
    print(img_text)
    # img_text = pytesseract.image_to_data(img, lang="eng")
    # print(img_text)
    # Geht nicht
    # img_text = pytesseract.image_to_osd(img, config='--psm 0', lang="deu")

