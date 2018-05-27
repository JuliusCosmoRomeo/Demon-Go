package com.github.demongo.opencv.pipeline;

import org.opencv.core.Mat;

public class Snapshot {
    public Mat mat;
    public double blurScore;
    public double noiseScore;
    //pointcloud

    public Snapshot(Mat mat, double blurScore, double noiseScore){
        this.mat = mat;
        this.blurScore = blurScore;
        this.noiseScore = noiseScore;
    }


}
