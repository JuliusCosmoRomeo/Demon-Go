package hpi.gitlab.demongo.pipeline;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SendingStep extends StepWithQueue {
    private static final String TAG = SendingStep.class.getName();
    private static final String URL = "http://tmbe.me:8088";

    private RequestQueue requestQueue;
    private ScheduledExecutorService executorService;

    public SendingStep(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;

        Runnable senderRunnable = new Runnable() {
            @Override
            public void run() {
                Snapshot best = getBest();
                if (best != null){
                    sendImage(best);
                    Log.i(TAG, "bestScore: " + best.score);
                }
            }
        };

        this.executorService = Executors.newScheduledThreadPool(0);
        this.executorService.scheduleAtFixedRate(senderRunnable, 1, 500, TimeUnit.MILLISECONDS);
    }

    // TODO: Evaluate if/when this should be done
    public void cancelExecutorService() {
        this.executorService.shutdown();
    }

    private void sendImage(final Snapshot snapshot) {
        String url = URL + "/post";

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, "Server response: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,  error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return snapshot.getRequestParameterList();
            }
        };

        this.requestQueue.add(request);
        Log.e(TAG, "POST-request added: ");
    }


    @Override
    public void process(Snapshot last) {
        this.queue(last);
//        Log.i(TAG, "image added with score: " + last.score);
        this.output(last);
    }
}
