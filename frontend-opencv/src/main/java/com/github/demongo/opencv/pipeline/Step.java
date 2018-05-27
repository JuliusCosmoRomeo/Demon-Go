package com.github.demongo.opencv.pipeline;

import java.util.ArrayList;

public abstract class Step {
    Snapshot snapshot;

    Step(){
    }

    Step(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    protected void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Step next(Step nextStep){
        this.process();
        nextStep.setSnapshot(this.snapshot);
        return nextStep;
    }

    abstract public void process();

}
