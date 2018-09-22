package hpi.gitlab.demongo.pipeline;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;

public class Pipeline {

    private BrandDetectionStep brandDetectionStep;
    private NoiseEstimationStep noiseEstimationStep;
    private ColorfulnessEstimationStep colorfulnessEstimationStep;
    private BlurEstimationStep blurEstimationStep;
    private ContourDetectionStep contourDetectionStep;
    private DirectSendStep directSendStep;
    private Step firstStep;
    private SendingStep sendingStep;
    private CircularFifoQueue<Snapshot> nextFrames;
    private final HashMap<String, ArrayList<String>> objectTemplateNameMap = new HashMap<String, ArrayList<String>>(){{
        put("mate", new ArrayList<String>(){{
            add("mate_logo");
            add("mate_label");
            add("mate_flasche");
        }});
    }};

    public Pipeline(Context context, Step angleChangeStep) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        BrandDetectionStep brandDetectionStep = new BrandDetectionStep(context, objectTemplateNameMap);

        noiseEstimationStep = new NoiseEstimationStep();
        blurEstimationStep = new BlurEstimationStep();
        contourDetectionStep = new ContourDetectionStep();
        colorfulnessEstimationStep = new ColorfulnessEstimationStep();
        directSendStep = new DirectSendStep();
        sendingStep = new SendingStep(requestQueue, context);

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
               .next(brandDetectionStep)
               .next(sendingStep);
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

    // return {x0, y0, z0, x1, y1, z1, ...}
    public float[] requestTargets() {
        return new float[] {};
    }

    public Mat debugAdd(Snapshot snapshot) {
        Mat debug = snapshot.createDebugMat();
        firstStep.start(snapshot);
        return debug;
    }
}
