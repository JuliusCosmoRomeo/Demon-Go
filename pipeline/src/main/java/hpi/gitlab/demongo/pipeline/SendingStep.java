package hpi.gitlab.demongo.pipeline;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SendingStep extends StepWithQueue {

    private static final String TAG = SendingStep.class.getName();
//    private static final String URL = "http://139.59.145.241:5000";
    // private static final String URL = "http://tmbe.me:8088";
    private static final String URL = "http://pb8704.byod.hpi.de:5000";

    private RequestQueue requestQueue;
    private ScheduledExecutorService executorService;

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";


    SendingStep(RequestQueue requestQueue, Context context) {
        this.requestQueue = requestQueue;
        uniqueID = id(context);

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

    public synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);

            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.apply();
            }
        }
        return uniqueID;
    }

    // TODO: Evaluate if/when this should be done
    public void cancelExecutorService() {
        this.executorService.shutdown();
    }

    private void sendImage(final Snapshot snapshot) {
        String url = URL + "/detect_text";

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
                Map<String, String> params = snapshot.getRequestParameterList();
                params.put("user_id", uniqueID);
                Log.i(TAG, "getParams: " + uniqueID);
                return params;
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
