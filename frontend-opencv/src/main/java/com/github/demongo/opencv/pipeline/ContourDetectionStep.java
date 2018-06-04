package com.github.demongo.opencv.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;


import java.util.ArrayList;
import java.util.List;


public class ContourDetectionStep extends Step {
    private static final String TAG = ContourDetectionStep.class.getName();
    private static final double MIN_CONTOUR_SIZE = 700;

    private static Mat transform(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);
        Imgproc.GaussianBlur(src, src, new Size(9, 9), 0);
        Mat transformedMat = new Mat();
        double high_thresh = Imgproc.threshold(src, transformedMat, 70, 255, Imgproc.THRESH_BINARY);
        double low_thresh = 0.5 * high_thresh;

        Imgproc.Canny(src, transformedMat, low_thresh, high_thresh);

        return transformedMat;
    }

    public Mat find_contours(Mat src) {
        Mat transformedMat = ContourDetectionStep.transform(src.clone());

        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(transformedMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint thisContour = contours.get(i);
            double contourArea = Imgproc.contourArea(thisContour);

            if(contourArea > MIN_CONTOUR_SIZE) {
                MatOfPoint2f thisContour2f = new MatOfPoint2f();
                MatOfPoint approxContour = new MatOfPoint();
                MatOfPoint2f approxContour2f = new MatOfPoint2f();

                thisContour.convertTo(thisContour2f, CvType.CV_32FC2);

                Imgproc.approxPolyDP(thisContour2f, approxContour2f, 2, true);

                approxContour2f.convertTo(approxContour, CvType.CV_32S);

                Rect rect = Imgproc.boundingRect(approxContour);
//                Imgproc.rectangle(src, rect.tl(), rect.br(), new Scalar(255, 255, 0), 1, 8, 0);
//                Imgproc.drawContours(src, contours, i, new Scalar(0, 255, 255), -1);
                Mat roi = src.submat(rect);
                Snapshot newSnap = new Snapshot(roi, contourArea);
                this.output(newSnap);
            }
        }
        return src;
    }

    @Override
    public void process(Snapshot last) {
        this.find_contours(last.mat);
    }

}
