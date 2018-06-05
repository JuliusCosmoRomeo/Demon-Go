package com.github.demongo.opencv.pipeline;

import java.util.Comparator;
import java.util.PriorityQueue;

class SnapshotComparator implements Comparator<Snapshot> {
    @Override
    public int compare(Snapshot a, Snapshot b) {
        return (int) Math.signum(b.score - a.score);
    }
}

abstract class StepWithQueue extends Step {
    private PriorityQueue<Snapshot> snapshotQueue;

    StepWithQueue() {
       this.snapshotQueue = new PriorityQueue<>(10, new SnapshotComparator());
    }

    void queue(Snapshot snapshot) {
        snapshotQueue.add(snapshot);
    }

    Snapshot getBest() {
        return this.snapshotQueue.poll();
    }

    Snapshot getBestAndClear() {
        Snapshot best = this.snapshotQueue.peek();
        this.snapshotQueue.clear();
        return best;
    }
}
