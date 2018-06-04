import cv2


def fetch_stream():
    logo = cv2.imread('club_mate_logo_x25.png', 0)

    # Initialize ORB detector
    detector = cv2.ORB_create(
        edgeThreshold=1,
        nfeatures=10000,
        scoreType=cv2.ORB_FAST_SCORE
    )

    kp2, des2 = detector.detectAndCompute(logo, None)
    logo_keypoints = cv2.drawKeypoints(
        logo, kp2, None, color=(0, 255, 0),
        flags=cv2.DrawMatchesFlags_DEFAULT
    )
    cv2.imshow('logo', logo_keypoints)

    bf = cv2.BFMatcher(
        cv2.NORM_HAMMING,
        crossCheck=True
    )

    cv2.namedWindow('Video')
    cam = cv2.VideoCapture(0)

    while True:
        ret_val, img = cam.read()
        # img = cv2.flip(img, 1)
        kp1, des1 = detector.detectAndCompute(img, None)
        # img_keypoints = cv2.drawKeypoints(
        #     img, kp1, None, color=(0, 255, 0),
        #     flags=cv2.DrawMatchesFlags_DEFAULT
        # )
        # cv2.imshow('Keys', img_keypoints)

        matches = bf.match(des1, des2)

        matches = [x for x in matches if x.distance < 25]

        img = cv2.drawMatches(
            img, kp1, logo_keypoints, kp2, matches, None, flags=2)

        cv2.imshow('Video', img)

        key = cv2.waitKey(1)
        if key == 27:
            exit(0)


def main():
    fetch_stream()


if __name__ == '__main__':
    main()
