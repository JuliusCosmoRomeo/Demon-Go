package com.github.demongo.opencv.pipeline;

import java.util.ArrayList;

public abstract class Step {
    private ArrayList<Step> nextSteps;

    public Step(){
        nextSteps = new ArrayList<Step>();
    }

    public Step next(Step next){
        nextSteps.add(next);
        return next;
    }

    abstract public void process(Snapshot last);

    protected void output(Snapshot snap){
        for (Step next : nextSteps){
            next.process(snap);
        }
    }

}
