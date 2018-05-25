package com.github.demongo.opencv.pipeline;

import org.opencv.core.Mat;

public class Snapshot {
    Mat mat;
    double score;
    //pointcloud

    public Snapshot(Mat mat, double score){
        this.mat = mat;
        this.score = score;
    }


}
