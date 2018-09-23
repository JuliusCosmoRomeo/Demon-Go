import typing
from dataclasses import dataclass

import cv2

from estimators import (
    estimate_noise,
    estimate_lines,
    estimate_color_variation,
)


@dataclass
class Rectangle:
    x0 : int = 0
    y0 : int = 0
    x1 : int = 0
    y1 : int = 0
    finished : bool = False
    drawing : bool = False
    sigma : typing.Union[None, float] = None

    @property
    def pos(self):
        return ((self.x0, self.y0), (self.x1, self.y1))

    @property
    def heigth(self):
        return self.y1 - self.y0

    @property
    def width(self):
        return self.x1 - self.x0


    def draw_on(self, img):
        color = (255,0 + 255 * int(bool(self.sigma)),0)
        cv2.rectangle(img, *self.pos, color, 2)
        if self.sigma is not None:
            cv2.putText(
                img,
                f'{self.sigma}',
                (self.x0, self.y0 + 20),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1
            )

class RectangleDrawer:

    def __init__(self):
        self.rectangles = [Rectangle(drawing=False, finished=False)]
        self.processed = []
        self.next_pos = [0,0]
        self.current_frame = None

    def draw(self, img):
        for rect in self.rectangles[1:]:
            if rect.finished:
                crop = img[rect.y0:rect.y1, rect.x0:rect.x1]
                rect.sigma = estimate_color_variation(crop)

                name = f'Crop {len(self.processed)}'
                cv2.imshow(name, crop)
                cv2.moveWindow(name, *self.next_pos)
                self.next_pos[0] += rect.width + 1

                self.processed.append(rect)
                del self.rectangles[-1]
            else:
                rect.draw_on(img)
        for rect in self.processed:
            rect.draw_on(img)


    @property
    def newest(self):
        return self.rectangles[-1]

    def reset(self):
        for i in range(len(self.processed)):
            cv2.destroyWindow(f'Crop {i}')
        self.__init__()

    def on_mouse(self, event, x, y, flags, params):

        if event is cv2.EVENT_LBUTTONDOWN:
            self.rectangles.append(Rectangle())
            self.newest.finished = False
            self.newest.drawing = True
            self.newest.x0, self.newest.y0 = (x, y)
        elif event is cv2.EVENT_MOUSEMOVE and self.newest.drawing:
            self.newest.drawing = True
            self.newest.x1, self.newest.y1 = (x, y)
        elif event is cv2.EVENT_LBUTTONUP:
            rect = self.newest
            if (rect.x0 - x) and (rect.y0 - y):
                rect.x0, rect.x1 = (rect.x0, x) if rect.x0 < x else (x, rect.x0)
                rect.y0, rect.y1 = (rect.y0, y) if rect.y0 < y else (y, rect.y0)
                rect.finished = True
                rect.drawing = False
            else:
                self.rectangles.pop()
