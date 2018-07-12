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
    private long executionStart;
    private long executionCounter;
    private double executionTime;

    private static final boolean MEASURE_TIME = false;

    public Step(){
        nextSteps = new ArrayList<Step>();
        executionCounter = 0;
        executionTime = 0;
    }

    public void setNextFrames(CircularFifoQueue<Snapshot> nextFrames) {
        this.nextFrames = nextFrames;
    }

    public Step next(Step next){
        nextSteps.add(next);
        return next;
    }

    public void start(Snapshot last) {
        if (MEASURE_TIME) {
            executionStart = System.nanoTime();
            executionCounter += 1;
        }
        process(last);
    }

    public void run(){

        while(true){
            try {
                Snapshot snapshot = nextFrames.poll();
                if (snapshot != null) {
                    start(snapshot);
                }
            } catch(NoSuchElementException e){
                Log.e("demon-go-step", "queue is empty");
            }

        }
    }

    abstract public void process(Snapshot last);

    protected void output(Snapshot snap){
        if (MEASURE_TIME) {
            String className = this.getClass().getName();
            executionTime += (System.nanoTime() - executionStart) / 1e9;
            Log.i("demon-go-step", className + " Average Execution: " + executionTime/executionCounter);
        }
        for (Step next : nextSteps){
            next.start(snap);
        }
    }

}
