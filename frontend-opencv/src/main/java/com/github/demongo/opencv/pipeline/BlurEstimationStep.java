package com.github.demongo.opencv.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

public class BlurEstimationStep extends Step {
    private static final String TAG = BlurEstimationStep.class.getName();

    public BlurEstimationStep() {
    }

    public BlurEstimationStep(Snapshot snapshot) {
        super(snapshot);
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

    @Override
    public void process() {
        double blurriness = getBlurValue(this.snapshot.mat);
        Log.i(TAG, "process.blurriness: " + blurriness);

        this.snapshot.blurScore = blurriness;
    }
}
