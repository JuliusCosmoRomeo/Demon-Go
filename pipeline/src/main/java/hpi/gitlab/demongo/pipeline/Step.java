package hpi.gitlab.demongo.pipeline;

import android.util.Log;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import static java.lang.System.nanoTime;

public abstract class Step implements Runnable {
    private ArrayList<Step> nextSteps;
    private CircularFifoQueue<Snapshot> nextFrames;

    public void setMeasureTime(boolean measureTime) {
        this.measureTime = measureTime;
    }

    private boolean measureTime;
    public Step(){
        nextSteps = new ArrayList<Step>();
        measureTime = false;
    }

    public void setNextFrames(CircularFifoQueue<Snapshot> nextFrames) {
        this.nextFrames = nextFrames;
    }

    public Step next(Step next){
        nextSteps.add(next);
        return next;
    }


    public void run(){

        while(true){
            try {
                Snapshot snapshot = nextFrames.poll();
                if (snapshot != null) {
                    if (measureTime){
                        long start = System.nanoTime();
                        process(snapshot);
                        Log.i("demon-go-step", "Execution time for last frame in s " + (float)(System.nanoTime() - start) / 1e9);
                    } else {
                        process(snapshot);
                    }
                }
            } catch(NoSuchElementException e){
                Log.e("demon-go-step", "queue is empty");
            }

        }
    }

    abstract public void process(Snapshot last);

    void output(Snapshot snap){
        for (Step next : nextSteps){
            next.process(snap);
        }
    }

}
