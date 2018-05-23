import base64
from datetime import datetime

from flask import Flask, request

app = Flask(__name__)


@app.route('/post', methods=['GET', 'POST'])
def test():
    image_string = request.form['image']

    # TODO: currently overwrites pictures sent at the same time
    ts = datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
    with open("images/" + ts + ".jpg", "wb") as f:
        f.write(base64.b64decode(image_string))
    return 'gotcha'


if __name__ == '__main__':
    app.run(debug=True)
