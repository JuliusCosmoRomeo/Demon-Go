package com.github.demongo.opencv;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.core.MatOfPoint;

import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContourDrawer {
    public static Mat draw_contours(Mat src) {
        Log.d("demon-go", "drawing contours");
        Mat grayMat = new Mat();
        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(grayMat, grayMat, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(grayMat, grayMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,11,2);

        final List<MatOfPoint> contours = new ArrayList<>();
        final Mat hierarchy = new Mat();
        Imgproc.findContours(grayMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            if (Imgproc.contourArea(contours.get(i))>7000){
                MatOfPoint2f contour2f = new MatOfPoint2f( contour.toArray() );
                double peri = Imgproc.arcLength(contour2f,true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, approx, (peri * 0.04),true);

                if(approx.toList().size()==4){
                    List<MatOfPoint> approxList = new ArrayList<>();
                    approxList.add( new MatOfPoint(approx.toArray()));
                    Mat mask = new Mat(src.rows(), src.cols(), CvType.CV_8U, Scalar.all(0));

                    Imgproc.drawContours(mask,approxList, 0,new Scalar(255, 255, 255), -1);
                    Scalar meanColor = Core.mean(src, mask);
                    Log.d("demon-go", "mean color " + meanColor.toString());
                    //double maxChannelValue = Math.max(meanColor.val[0],Math.max(meanColor.val[1], meanColor.val[2]));
                    //double minChannelValue = Math.min(meanColor.val[0],Math.max(meanColor.val[1], meanColor.val[2]));

                   // if (meanColor.val[0] > 50 && meanColor.val[1] > 50 && meanColor.val[2] > 50){
                        Log.d("demon-go", "mean color grey");
                        Imgproc.drawContours(src, approxList, 0, new Scalar(255, 255, 0), 2);
                    //}


                }

                /*else {
                    Imgproc.drawContours(src, contours, i, new Scalar(255, 0, 0), 2);
                }*/

            }

        }

        return src;
    }
}