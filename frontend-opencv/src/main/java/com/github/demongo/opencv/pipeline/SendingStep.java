package com.github.demongo.opencv.pipeline;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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
    private static final String URL = "http://192.168.0.106:5000";

    private RequestQueue requestQueue;

    public SendingStep(Context context) {
        this.requestQueue = Volley.newRequestQueue(context);

        Runnable senderRunnable = new Runnable() {
            @Override
            public void run() {
                Snapshot best = getBest();
                if (best != null){
                    sendImage(best.mat);
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(senderRunnable, 1, 2, TimeUnit.SECONDS);
    }

    private String matToBase64String(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        // TODO: convert directly to jpeg
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
                Log.i(TAG, response);
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
//        if(last.score > 0.8) { //Will later be replaced and Frames will be selected via queue
//            this.sendImage(last.mat);
//        }
        this.queue(last);
        this.output(last);
    }
}
