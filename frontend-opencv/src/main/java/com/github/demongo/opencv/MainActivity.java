package com.github.demongo.opencv;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.github.demongo.opencv.pipeline.BlurEstimationStep;
import com.github.demongo.opencv.pipeline.BrandDetectionStep;
import com.github.demongo.opencv.pipeline.ContourDetectionStep;
import com.github.demongo.opencv.pipeline.NoiseEstimationStep;
import com.github.demongo.opencv.pipeline.SendingStep;
import com.github.demongo.opencv.pipeline.Snapshot;
import com.github.demongo.opencv.pipeline.Step;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    private CameraBridgeViewBase openCvCameraView;
    private Mat currentFrame;
    private BrandDetectionStep brandDetectionStep;
    private NoiseEstimationStep noiseEstimationStep;
    private BlurEstimationStep blurEstimationStep;
    private ContourDetectionStep contourDetectionStep;
    private Step firstStep;
    SendingStep sendingStep;
    private RequestQueue requestQueue;
    private CircularFifoQueue<Mat> nextFrames;
    private static final String TAG = MainActivity.class.getName();

    //this callback is needed because Android's onCreate is called before OpenCV is loaded
    //-> hence Mat-initialization with "new Mat()" fails in onCreate
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                    brandDetectionStep = new BrandDetectionStep(getApplicationContext(), new ArrayList<String>(){{
                        add("template");
                        add("mate_label");
                        add("mate_flasche");
                    }});

                    noiseEstimationStep = new NoiseEstimationStep();
                    blurEstimationStep = new BlurEstimationStep();
                    contourDetectionStep = new ContourDetectionStep();
                    sendingStep = new SendingStep(requestQueue);

                    blurEstimationStep
                            .next(contourDetectionStep)
                            .next(noiseEstimationStep)
                            .next(sendingStep);
                    blurEstimationStep
                            .next(brandDetectionStep)
                            .next(sendingStep);
                    nextFrames = new CircularFifoQueue<>(10);
                    openCvCameraView.enableView();

                    firstStep = blurEstimationStep;
                    firstStep.setMeasureTime(true);
                    firstStep.setNextFrames(nextFrames);
                    Thread pipelineThread = new Thread(firstStep);
                    pipelineThread.start();


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

        this.requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        openCvCameraView.disableView();

        this.sendingStep.cancelExecutorService();
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
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        currentFrame = frame.rgba();

        nextFrames.add(currentFrame);
        //blurEstimationStep.process(new Snapshot(currentFrame,1));

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
}
