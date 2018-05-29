package com.github.demongo.opencv.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class NoiseEstimationStep extends Step {
    private static final String TAG = NoiseEstimationStep.class.getName();

    private final Mat kernel = new Mat(3, 3, CvType.CV_32F) {
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


    private double estimateNoise(Mat image){
        Mat grayMat = new Mat();
        Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY);
        int H = image.height();
        int W = image.width();


        Mat destination = new Mat(image.rows(),image.cols(),image.type());
        Imgproc.filter2D(image, destination, -1, this.kernel);
        Core.absdiff(destination, Scalar.all(0), destination);
        double total = Core.sumElems(destination).val[0];
        Log.i(TAG, "estimateNoise.total: " + Double.toString(total));

        total = total * Math.sqrt(0.5 * Math.PI) / (6 * (W-2) * (H-2));
        return total;
    }

    @Override
    public void process(Snapshot last) {
        double noisiness = this.estimateNoise(last.mat);
        if (noisiness>1.5) {
            Snapshot newSnap = new Snapshot(last.mat,noisiness);
            Log.i(TAG, "process.noisiness: " + noisiness);

            //this.output(newSnap);
        }
    }

}
