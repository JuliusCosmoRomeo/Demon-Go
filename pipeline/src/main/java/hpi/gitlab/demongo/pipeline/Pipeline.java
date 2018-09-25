package hpi.gitlab.demongo.pipeline;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Map;

public class Pipeline {
    private static final String TAG = "demon-go-Pipeline";

    private BrandDetectionStep brandDetectionStep;
    private NoiseEstimationStep noiseEstimationStep;
    private ColorfulnessEstimationStep colorfulnessEstimationStep;
    private BlurEstimationStep blurEstimationStep;
    private ContourDetectionStep contourDetectionStep;
    private DirectSendStep directSendStep;
    private Step firstStep;
    private SendingStep sendingStep;
    private CircularFifoQueue<Snapshot> nextFrames;
    private ArrayList<Float> targets = new ArrayList<Float>();
    private RequestQueue requestQueue;

    public Pipeline(Context context, Step angleChangeStep) {
        this.requestQueue = Volley.newRequestQueue(context);

        BrandDetectionStep brandDetectionStep = new BrandDetectionStep(context, requestQueue);

        noiseEstimationStep = new NoiseEstimationStep();
        blurEstimationStep = new BlurEstimationStep();
        contourDetectionStep = new ContourDetectionStep();
        colorfulnessEstimationStep = new ColorfulnessEstimationStep();
        directSendStep = new DirectSendStep();
        sendingStep = new SendingStep(this.requestQueue, context, this);

        blurEstimationStep
                .next(angleChangeStep);
        angleChangeStep
                .next(directSendStep)
                .next(sendingStep);
        angleChangeStep
                .next(contourDetectionStep)
                .next(colorfulnessEstimationStep)
                .next(noiseEstimationStep)
                .next(sendingStep);

       angleChangeStep
               .next(brandDetectionStep);
               // .next(sendingStep);
        nextFrames = new CircularFifoQueue<>(10);

        firstStep = blurEstimationStep;
        firstStep.setNextFrames(nextFrames);
        Thread pipelineThread = new Thread(firstStep);
        pipelineThread.start();

    }

    public void destroy() {
        this.sendingStep.cancelExecutorService();
    }

    public void add(Snapshot snapshot) {
        nextFrames.add(snapshot);
    }

    public void sendImmediately(final Snapshot snapshot) {
        String url = "http://139.59.145.241:5000/ocr";
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "sendImmediatelyError: " + error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = snapshot.getRequestParameterList();
                return params;
            }
        };

        this.requestQueue.add(request);
    }

    public void addTarget(ArrayList<Float> targetCoordinates) {
        Log.i(TAG, "addTarget: " + String.valueOf(targetCoordinates));
        this.targets.addAll(targetCoordinates);
    }

    // return {x0, y0, z0, x1, y1, z1, ...}
    public Float[] requestTargets() {
        Log.i(TAG,"requestTargets: " + this.targets.size());
        return this.targets.toArray(new Float[0]);
    }

    public Mat debugAdd(Snapshot snapshot) {
        Mat debug = snapshot.createDebugMat();
        firstStep.start(snapshot);
        return debug;
    }
}
