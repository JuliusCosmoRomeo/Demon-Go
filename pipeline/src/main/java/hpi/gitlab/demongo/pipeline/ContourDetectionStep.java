package hpi.gitlab.demongo.pipeline;

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
    private static final double CONTOUR_EDGES = 4;

    private static final boolean USE_BILATERAL = true;

    private static Mat transform(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGRA2BGR);
        Imgproc.GaussianBlur(src, src, new Size(9, 9), 0);
        Mat transformedMat = new Mat();
        double high_thresh = Imgproc.threshold(src, transformedMat, 70, 255, Imgproc.THRESH_BINARY);
        double low_thresh = 0.5 * high_thresh;

        if (USE_BILATERAL) {
            Imgproc.bilateralFilter(src, transformedMat, 11, 17, 17);
            src = transformedMat;
            transformedMat = new Mat();
        }

        Imgproc.Canny(src, transformedMat, low_thresh, high_thresh);

        return transformedMat;
    }

    public Mat find_contours(Snapshot snap) {
        Mat transformedMat = ContourDetectionStep.transform(snap.mat.clone());

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

                double epsilon = 0.01*Imgproc.arcLength(new MatOfPoint2f(thisContour.toArray()),true);
                Imgproc.approxPolyDP(thisContour2f, approxContour2f, epsilon, true);

                approxContour2f.convertTo(approxContour, CvType.CV_32S);


                if (approxContour.size().height > 2 && approxContour.size().height < 7) {

                    Rect rect = Imgproc.boundingRect(approxContour);
                    final List<MatOfPoint> approx = new ArrayList<>();
                    approx.add(approxContour);


                    if (snap.isDebug()) {
//                        snap.setDebugMat(transformedMat);
                        Imgproc.rectangle(snap.getDebugMat(), rect.tl(), rect.br(), new Scalar(255, 255, 0), 2, 8, 0);
                        Imgproc.drawContours(snap.getDebugMat(), contours, i, new Scalar(0, 255, 255), 3);
                        Imgproc.drawContours(snap.getDebugMat(), approx, 0, new Scalar(255, 0, 0), 2);
                    }

                    Mat roi = snap.mat.submat(rect);
                    this.output(snap.copyWithNewMat(roi));
                }
            }
        }
        return transformedMat;
    }

    @Override
    public void process(Snapshot last) {
        this.find_contours(last);
    }

}
