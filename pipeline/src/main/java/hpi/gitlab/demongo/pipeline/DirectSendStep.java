package hpi.gitlab.demongo.pipeline;

import android.util.Log;

public class DirectSendStep extends Step {
    private static final String TAG = "demon-go-direct-send";

    private int frame_counter = 0;

    @Override
    public void process(Snapshot last) {
        frame_counter += 1;
        Log.i(TAG, "process.frame_send: " + frame_counter);

        if (frame_counter == 100) {
            frame_counter = 0;
            this.output(last);
        }
    }
}
