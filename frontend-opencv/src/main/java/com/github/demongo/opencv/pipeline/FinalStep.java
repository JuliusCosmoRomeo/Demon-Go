package com.github.demongo.opencv.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class FinalStep extends Step {
    private Snapshot bestSnapshot;
    private static final String TAG = FinalStep.class.getName();
    private boolean newBestSnapshot = false;

    public FinalStep() {

        this.bestSnapshot = new Snapshot(new Mat(), 0, 0);
    }

    public void onNewSnapshot(Snapshot snapshot) {
        double blur = snapshot.blurScore;
        double noise = snapshot.noiseScore;

        Imgproc.putText(snapshot.mat,
                "Blur: " + Double.toString(blur),
                new Point(10, 50),
                Core.FONT_HERSHEY_SIMPLEX ,
                1,
                new Scalar(0, 0, 0),
                4);

        Imgproc.putText(snapshot.mat,
                "Noise: " + Double.toString(noise),
                new Point(10, 75),
                Core.FONT_HERSHEY_SIMPLEX ,
                1,
                new Scalar(0, 0, 0),
                4);

        if(this.bestSnapshot.blurScore < blur) {
            double blurChange = blur - this.bestSnapshot.blurScore;

            this.bestSnapshot.blurScore = blur;
            this.bestSnapshot.noiseScore = noise;
            this.bestSnapshot.mat = snapshot.mat;

            if(this.bestSnapshot.blurScore > 100 && blurChange > 5) {
                Log.e(TAG, "blurScore + Change" + this.bestSnapshot.blurScore + ", " + blurChange);
            }

            this.newBestSnapshot = true;
        } else {
            this.newBestSnapshot = false;
        }
    }

    public Snapshot getSnapshot() {
        if (newBestSnapshot) {
            return bestSnapshot;
        } else {
            return null;
        }
    }

    protected void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
        this.onNewSnapshot(snapshot);
    }

    @Override
    public void process() {

    }
}
