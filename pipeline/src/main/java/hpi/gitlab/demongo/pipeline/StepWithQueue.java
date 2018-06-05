package hpi.gitlab.demongo.pipeline;

import java.util.Comparator;
import java.util.PriorityQueue;

class SnapshotComparator implements Comparator<Snapshot> {
    @Override
    public int compare(Snapshot a, Snapshot b) {
        return (int) Math.signum(b.score - a.score);
    }
}

public abstract class StepWithQueue extends Step {
    private PriorityQueue<Snapshot> snapshotQueue;

    protected StepWithQueue() {
       this.snapshotQueue = new PriorityQueue<>(10, new SnapshotComparator());
    }

    protected void queue(Snapshot snapshot) {
        snapshotQueue.add(snapshot);
    }

    protected Snapshot getBest() {
        return this.snapshotQueue.poll();
    }

    protected Snapshot getBestAndClear() {
        Snapshot best = this.snapshotQueue.peek();
        this.snapshotQueue.clear();
        return best;
    }
}
