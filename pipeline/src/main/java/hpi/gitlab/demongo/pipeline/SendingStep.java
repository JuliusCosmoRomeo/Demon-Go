package hpi.gitlab.demongo.pipeline;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SendingStep extends StepWithQueue {
    private static final String TAG = SendingStep.class.getName();
//    private static final String URL = "http://139.59.145.241:5000";
//    private static final String URL = "http://tmbe.me:8088";
    private static final String URL = "http://10.42.0.1:5000";

    private RequestQueue requestQueue;
    private ScheduledExecutorService executorService;

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    private Pipeline pipeline;


    SendingStep(RequestQueue requestQueue, Context context, Pipeline _pipeline) {
        this.requestQueue = requestQueue;
        pipeline = _pipeline;
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
//        String url = URL + "/post";
        Log.i(TAG, "snapClass: " + snapshot.getClass());
        String url = URL + "/test";

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, "Server response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    double x = (double) jsonResponse.get("x");
                    double y = (double) jsonResponse.get("y");
                    Log.i(TAG, "onResponse: " + x + ", " + y);
                    ArrayList<Float> targetCoordinates = snapshot.processServerResponse((float) x , (float) y);
                    if(targetCoordinates != null) {
                       pipeline.addTarget(targetCoordinates);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        Log.i(TAG, "POST-request added: ");
    }

    @Override
    public void process(Snapshot last) {
        this.queue(last);
//        Log.i(TAG, "image added with score: " + last.score);
        this.output(last);
    }
}
