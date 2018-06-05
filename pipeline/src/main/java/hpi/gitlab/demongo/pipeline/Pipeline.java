package hpi.gitlab.demongo.pipeline;

import android.content.Context;

import java.util.ArrayList;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.opencv.core.Mat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class Pipeline {

    private BrandDetectionStep brandDetectionStep;
    private NoiseEstimationStep noiseEstimationStep;
    private BlurEstimationStep blurEstimationStep;
    private ContourDetectionStep contourDetectionStep;
    private Step firstStep;
    SendingStep sendingStep;
    private RequestQueue requestQueue;
    private CircularFifoQueue<Snapshot> nextFrames;

    public Pipeline(Context context) {
        this.requestQueue = Volley.newRequestQueue(context);

        brandDetectionStep = new BrandDetectionStep(context, new ArrayList<String>(){{
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

        firstStep = blurEstimationStep;
        firstStep.setMeasureTime(true);
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
}
