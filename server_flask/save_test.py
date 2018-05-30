import os

filename = "images/2"
ending = ".txt"
suffix = ""
if os.path.isfile(filename + ending):
    index = 1
    suffix = "(" + str(index) + ")"
    while os.path.isfile(filename + suffix + ending):
        index += 1
        suffix = "(" + str(index) + ")"

with open(filename + suffix + ending, "w") as f:
    f.write("test")

