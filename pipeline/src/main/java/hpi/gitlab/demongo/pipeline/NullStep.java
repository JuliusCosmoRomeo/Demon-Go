package hpi.gitlab.demongo.pipeline;

public class NullStep extends Step {

    @Override
    public void process(Snapshot last) {
        this.output(last);
    }

}
