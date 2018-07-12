package hpi.gitlab.demongo.pipeline;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.core.Point;

import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ColorfulnessEstimationStep extends Step {
    private static final String TAG = "demon-go-colored-step";
    private static final float THRESHOLD = 90f;

    private double estimateColorfulness(Mat image){
        List<Mat> channels = new ArrayList<Mat>(3);
        Core.split(image, channels);
        Mat R = channels.get(0);
        Mat G = channels.get(1);
        Mat B = channels.get(2);
        MatOfDouble rbMean = new MatOfDouble();
        MatOfDouble rbStd = new MatOfDouble();
        MatOfDouble ybMean = new MatOfDouble();
        MatOfDouble ybStd = new MatOfDouble();

        Mat rg = new Mat();
        Core.absdiff(R, G, rg);

        Mat rg_intermediate = new Mat();
        Core.addWeighted(R, 0.25, G, 0.25, 0, rg_intermediate);
        Mat yb = new Mat();
        Core.absdiff(rg_intermediate, B, yb);

        Core.meanStdDev(rg, rbMean, rbStd);
        Core.meanStdDev(yb, ybMean, ybStd);

        double rbmu = rbStd.get(0,0)[0];
        double ybmu = ybStd.get(0,0)[0];
        double rbsigma = rbMean.get(0,0)[0];
        double ybsigma = ybMean.get(0,0)[0];

        double stdRoot = Math.sqrt((rbmu * rbmu) + (ybmu * ybmu));
        double meanRoot = Math.sqrt((rbsigma * rbsigma) + (ybsigma * ybsigma));

        return stdRoot + (0.3 * meanRoot);

    }

    @Override
    public void process(Snapshot last) {
        double colorfulness = this.estimateColorfulness(last.mat);
        if (last.isDebug()) {
            Scalar color;
            Point offset = new Point();
            Size wholesize = new Size();
            last.mat.locateROI(wholesize,offset);
            if (colorfulness < THRESHOLD) {
                color = new Scalar(0, 255, 0);
            } else {
                color = new Scalar(255, 0, 0);
            }
            Imgproc.putText (
                    last.getDebugMat(),                          // Matrix obj of the image
                    Double.toString(colorfulness),          // Text to be added
                    offset,               // point
                    Core.FONT_HERSHEY_SIMPLEX ,      // front face
                    1,                               // front scale
                    color,
                    4                                // Thickness
            );
        }
        if (colorfulness < THRESHOLD) {
            Snapshot newSnap;
            if (last.score > 0) {
                newSnap = last.copyWithNewScore(colorfulness * last.score); //TODO: evaluate metric
            } else {
                newSnap = last.copyWithNewScore(colorfulness * last.score);
            }

            Log.i(TAG, "colorfulness: " + colorfulness);
            this.output(newSnap);
        }
    }

}
