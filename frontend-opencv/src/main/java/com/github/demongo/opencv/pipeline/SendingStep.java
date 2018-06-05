package com.github.demongo.opencv.pipeline;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SendingStep extends StepWithQueue {
    private static final String TAG = SendingStep.class.getName();
    private static final String URL = "http://10.42.0.1:5000";

    private RequestQueue requestQueue;
    private ScheduledExecutorService executorService;

    public SendingStep(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;

        Runnable senderRunnable = new Runnable() {
            @Override
            public void run() {
                Snapshot best = getBest();
                if (best != null){
                    sendImage(best.mat);
                    Log.i(TAG, "bestScore: " + best.score);
                }
            }
        };

        this.executorService = Executors.newScheduledThreadPool(0);
        this.executorService.scheduleAtFixedRate(senderRunnable, 1, 2, TimeUnit.SECONDS);
    }

    // TODO: Evaluate if/when this should be done
    public void cancelExecutorService() {
        this.executorService.shutdown();
    }

    private String matToBase64String(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 92, stream);
        byte[] imageBytes = stream.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }


    private void sendImage(final Mat mat) {
        String url = URL + "/post";

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, "Server response: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,  error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("image", matToBase64String(mat));
                return parameters;
            }
        };

        this.requestQueue.add(request);
        Log.e(TAG, "POST-request added");
    }


    @Override
    public void process(Snapshot last) {
        this.queue(last);
//        Log.i(TAG, "image added with score: " + last.score);
        this.output(last);
    }
}
