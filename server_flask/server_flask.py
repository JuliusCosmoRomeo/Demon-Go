import base64

from flask import Flask, request

app = Flask(__name__)


@app.route('/post', methods=['GET', 'POST'])
def test():
    image_string = request.form['image']
    print(image_string)
    with open("images/test.jpg", "wb") as f:
        f.write(base64.b64decode(image_string))
    return 'test'


if __name__ == '__main__':
    app.run(debug=True)
