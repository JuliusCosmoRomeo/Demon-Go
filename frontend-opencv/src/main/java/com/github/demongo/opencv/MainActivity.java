package com.github.demongo.opencv;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private CameraBridgeViewBase openCvCameraView;
    private Mat currentFrame;
    private TemplateMatching templateMatching;

    //this callback is needed because Android's onCreate is called before OpenCV is loaded
    //-> hence Mat-initialization with "new Mat()" fails in onCreate

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                    //if you want to test with less images to enhance performance simply
                    templateMatching = new TemplateMatching(getApplicationContext(), new ArrayList<String>(){{
                        add("template");
                        add("mate_label");
                        add("mate_flasche");

                    }});
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCameraPermission();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        openCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCvCameraView.setCvCameraViewListener(this);

    }

    @Override
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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("demon-go", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        openCvCameraView.enableView();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        //Log.e("demon-go", "ON CAMERA FRAME");
        currentFrame = frame.rgba();

        /*Imgproc.putText(currentFrame,
                Double.toString(getBlurValue(currentFrame)),
                new Point(10, 50),
                Core.FONT_HERSHEY_SIMPLEX ,
                1,
                new Scalar(0, 0, 0),
                4);

        Imgproc.putText(currentFrame,
                Double.toString(estimateNoise(currentFrame)),
                new Point(10, 70),
                Core.FONT_HERSHEY_SIMPLEX ,
                1,
                new Scalar(0, 0, 0),
                4);
*/

        //        return currentFrame;
        if (templateMatching!= null){
            //currentFrame = templateMatching.findTemplate(currentFrame);
            currentFrame = templateMatching.matchFeatures(currentFrame);
        }
        //return ContourDrawer.draw_contours(currentFrame);
        return currentFrame;



    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.e("demon-go", "ON CAMERA VIEW STARTED");
        currentFrame = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        currentFrame.release();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
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
        //Log.e("demon-go", Double.toString(blurValue));

        return blurValue;
    }

    public double estimateNoise(Mat image) {
        Mat grayMat = new Mat();
        Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY);
        int H = image.height();
        int W = image.width();

        Mat kernel = new Mat(3, 3, CvType.CV_32F) {
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

        Mat destination = new Mat(image.rows(),image.cols(),image.type());
        Imgproc.filter2D(image, destination, -1, kernel);
        Core.absdiff(destination, Scalar.all(0), destination);
        double total = Core.sumElems(destination).val[0];
        Log.e("demon-go", Double.toString(total));

        total = total * Math.sqrt(0.5 * Math.PI) / (6 * (W-2) * (H-2));

        return total;

    }


}
