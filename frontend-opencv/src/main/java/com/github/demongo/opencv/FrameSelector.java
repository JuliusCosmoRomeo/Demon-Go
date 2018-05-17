package com.github.demongo.opencv;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class FrameSelector {

    private AnalysedFrame bestFrame;
    private ImageView imageView;

    FrameSelector() {
        this.bestFrame = new AnalysedFrame();
        this.bestFrame.noise = 0.0;
        this.bestFrame.blur = 0.0;

        this.imageView = imageView;
    }

    private double estimateNoise(Mat image) {
        Mat grayMat = new Mat();
        Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY);
        int H = image.height();
        int W = image.width();

        Mat kernel = new Mat(3, 3, CvType.CV_32F) {
            {
                put(0,0,1);
                put(0,1,-2);
                put(0,2,1);

                put(1,0,-2);
                put(1,1,4);
                put(1,2,-2);

                put(2,0,1);
                put(2,1,-2);
                put(2,2,1);

            }
        };

        Mat destination = new Mat(image.rows(),image.cols(),image.type());
        Imgproc.filter2D(image, destination, -1, kernel);
        Core.absdiff(destination, Scalar.all(0), destination);
        double total = Core.sumElems(destination).val[0];
        // Log.e("demon-go", Double.toString(total));

        total = total * Math.sqrt(0.5 * Math.PI) / (6 * (W-2) * (H-2));

        return total;

    }

    private double getBlurValue(Mat image) {
        Mat gray = new Mat();
        Mat destination = new Mat();

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(gray, destination, 3);

        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(destination, median , std);

        return Math.pow(std.get(0,0)[0],2);
    }

    public Mat newFrame(Mat frame) {
        double blur = getBlurValue(frame);
        double noise = estimateNoise(frame);

        if(bestFrame.frame == null) {
            bestFrame.frame = frame;
        }

        if(this.bestFrame.blur < blur) {
            bestFrame.blur = blur;
            bestFrame.noise = noise;
            bestFrame.frame = frame;
        }

        Mat tmpFrame = bestFrame.frame;

        Imgproc.putText(tmpFrame,
                "Blur: " + Double.toString(blur),
                new Point(10, 50),
                Core.FONT_HERSHEY_SIMPLEX ,
                1,
                new Scalar(0, 0, 0),
                4);

        Imgproc.putText(tmpFrame,
                "Noise: " + Double.toString(noise),
                new Point(10, 75),
                Core.FONT_HERSHEY_SIMPLEX ,
                1,
                new Scalar(0, 0, 0),
                4);

        return tmpFrame;
    }

}
