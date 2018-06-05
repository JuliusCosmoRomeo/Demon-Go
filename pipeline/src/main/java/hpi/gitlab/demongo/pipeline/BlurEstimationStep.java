package hpi.gitlab.demongo.pipeline;

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

        double totalBlurriness = Math.pow(std.get(0,0)[0],2);
        return totalBlurriness > 500 ? 1 : totalBlurriness/500;
    }

    @Override
    public void process(Snapshot last) {
        double blurriness = getBlurValue(last.mat);
        Log.i(TAG, "process.blurriness: " + blurriness);
        if (blurriness>=0.2) {

            Snapshot newSnap = new Snapshot(last.mat, blurriness);
            this.output(newSnap);
        }
    }
}
