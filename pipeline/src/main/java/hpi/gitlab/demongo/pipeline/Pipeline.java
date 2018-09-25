package hpi.gitlab.demongo.pipeline;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;

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
    private final HashMap<String, ArrayList<BrandDetectionStep.BrandLogo>> objectTemplateNameMap = new HashMap<String, ArrayList<BrandDetectionStep.BrandLogo>>(){{
        put("mate", new ArrayList<BrandDetectionStep.BrandLogo>(){{
            add(new BrandDetectionStep.BrandLogo("mate_logo", 25));
            add(new BrandDetectionStep.BrandLogo("mate_label", 25));
            add(new BrandDetectionStep.BrandLogo("mate_flasche", 25));
            add(new BrandDetectionStep.BrandLogo("club_mate_logo_x25", 25));
        }});
        put("ahoj_brause", new ArrayList<BrandDetectionStep.BrandLogo>(){{
            add(new BrandDetectionStep.BrandLogo("ahoj_brause_logo", 20));
        }});
        put("thinkpad", new ArrayList<BrandDetectionStep.BrandLogo>(){{
            add(new BrandDetectionStep.BrandLogo("thinkpad-logo-white", 25));
        }});
    }};
    private ArrayList<Float> targets = new ArrayList<Float>();

    public Pipeline(Context context, Step angleChangeStep) {
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        BrandDetectionStep brandDetectionStep = new BrandDetectionStep(context, objectTemplateNameMap);

        noiseEstimationStep = new NoiseEstimationStep();
        blurEstimationStep = new BlurEstimationStep();
        contourDetectionStep = new ContourDetectionStep();
        colorfulnessEstimationStep = new ColorfulnessEstimationStep();
        directSendStep = new DirectSendStep();
        sendingStep = new SendingStep(requestQueue, context, this);

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

    public void sendImmediately(Snapshot snapshot) {
        sendingStep.queue(snapshot);
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
