package hpi.gitlab.demongo.pipeline;

import org.opencv.core.Mat;

public class Snapshot {
    protected Mat mat;
    protected double score;

    public Snapshot(Mat mat, double score){
        this.mat = mat;
        this.score = score;
    }

    public Snapshot copyWith(Mat mat, double score) {
        return new Snapshot(mat, score);
    }

    public Snapshot copyWithNewMat(Mat newMat) {
        return new Snapshot(newMat, score);
    }

    public Snapshot copyWithNewScore(double newScore) {
        return new Snapshot(mat, newScore);
    }
}
