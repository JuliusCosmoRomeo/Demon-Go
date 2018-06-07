package hpi.gitlab.demongo.pipeline;

import org.opencv.core.Mat;

public class Snapshot {
    protected Mat mat;
    protected double score;
    private Mat debugMat;

    public Snapshot(Mat mat, double score, Mat debugMat) {
        this.mat = mat;
        this.score = score;
        this.debugMat = debugMat;
    }

    public Snapshot(Mat mat, double score) {
        this(mat, score, null);
    }

    public Snapshot copyWith(Mat mat, double score) {
        return new Snapshot(mat, score, debugMat);
    }

    public Snapshot copyWithNewMat(Mat newMat) {
        return new Snapshot(newMat, score, debugMat);
    }

    public Snapshot copyWithNewScore(double newScore) {
        return new Snapshot(mat, newScore, debugMat);
    }

    public Mat createDebugMat() {
        debugMat = mat.clone();
        return debugMat;
    }

    public Mat getDebugMat() {
        return debugMat;
    }

    public boolean isDebug() {
        return (getDebugMat() != null);
    }
}
