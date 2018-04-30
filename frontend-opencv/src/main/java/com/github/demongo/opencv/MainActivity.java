package com.github.demongo.opencv;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private CameraBridgeViewBase openCvCameraView;
    private Mat currentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        openCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);
    }

    public void onDestroy() {
        super.onDestroy();
        openCvCameraView.disableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (openCvCameraView != null)
            openCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("demon-go", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, null);
        } else {
            Log.d("demon-go", "OpenCV library found inside package. Using it!");
            // mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        currentFrame = frame.rgba();

        Imgproc.rectangle(currentFrame, new Point(10, 10), new Point(80, 80), new Scalar(0, 255, 0, 255), 3);
        return currentFrame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        currentFrame = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        currentFrame.release();
    }

    public double getBlurValue(Mat image) {
        Mat gray = new Mat();
        Mat destination = new Mat();

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(gray, destination, 3);

        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(destination, median , std);

        double blurValue = Math.pow(std.get(0,0)[0],2);
        Log.e("demon-go", Double.toString(blurValue));

        return blurValue;
    }

    public double getBlurValue(Mat image) {
        Mat gray = new Mat();
        Mat destination = new Mat();

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Laplacian(gray, destination, 3);

        MatOfDouble median = new MatOfDouble();
        MatOfDouble std= new MatOfDouble();
        Core.meanStdDev(destination, median , std);

        double blurValue = Math.pow(std.get(0,0)[0],2);
        Log.e("demon-go", Double.toString(blurValue));

        return blurValue;
    }

}
