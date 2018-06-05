package hpi.gitlab.demongo.pipeline;

import org.opencv.core.Mat;

public class Snapshot {
    Mat mat;
    double score;

    public Snapshot(Mat mat, double score){
        this.mat = mat;
        this.score = score;
    }


}
