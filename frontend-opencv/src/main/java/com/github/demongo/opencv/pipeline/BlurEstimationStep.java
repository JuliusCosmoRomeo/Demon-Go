package com.github.demongo.opencv.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

public class BlurEstimationStep extends Step {
    private static final String TAG = BlurEstimationStep.class.getName();

    private double getBlurValue(Mat image) {
        Mat gray = new Mat();
        Mat destination = new Mat();

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(gray, destination, 3);

        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(destination, median , std);

        double totalBluriness = Math.pow(std.get(0,0)[0],2);
        return totalBluriness > 500 ? 1 : totalBluriness/500;
    }

    @Override
    public void process(Snapshot last) {
        double blurriness = getBlurValue(last.mat);
        if (blurriness>=0.2) {
            Log.d(TAG, "process.blurriness: " + blurriness);

            Snapshot newSnap = new Snapshot(last.mat, blurriness);
            this.output(newSnap);
        }
    }
}
