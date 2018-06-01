package com.github.demongo.opencv.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;

import java.util.ArrayList;
import java.util.List;


public class ContourDetectionStep extends Step {
    private static final String TAG = ContourDetectionStep.class.getName();

    private static Mat transform(Mat src) {
        Mat transformedMat = new Mat()
        Imgproc.cvtColor(src, transformedMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(transformedMat, transformedMat, new Size(9, 9), 0);
        double high_thresh = Imgproc.threshold(transformedMat, transformedMat, 70, 255, Imgproc.THRESH_BINARY);
        double low_thresh = 0.5 * high_thresh;

        Imgproc.bilateralFilter(transformedMat, transformedMat, 11, 17, 17);
        Imgproc.Canny(transformedMat, transformedMat, low_thresh, high_thresh, 3);

        return transformedMat
    }

    private static Mat find_contours(Mat src) {
        Mat transformedMat = ContourDetectionStep.transform(src);

        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(transformedMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Log.i(TAG, "process.detected_contours: " + contours.size());

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f thisContour2f = new MatOfPoint2f();
            MatOfPoint approxContour = new MatOfPoint();
            MatOfPoint2f approxContour2f = new MatOfPoint2f();

            thisContour.convertTo(thisContour2f, CvType.CV_32FC2);

            Imgproc.approxPolyDP(thisContour2f, approxContour2f, 2, true);

            approxContour2f.convertTo(approxContour, CvType.CV_32S);

            if (approxContour.size().height == 4) {
                Imgproc.drawContours(src, contours, i, new Scalar(255, 255, 255), -1);
            }
        }
    }

    @Override
    public void process(Snapshot last) {
        Mat rectMat = ContourDetectionStep.transform(last.mat);
        Snapshot newSnap = new Snapshot(rectMat, last.score);
        this.output(newSnap);
    }

}
