package hpi.gitlab.demongo.pipeline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BrandDetectionStep extends Step {

    static final public class BrandLogo {
        final String logo_name;
        final Integer threshold;
        Template template;
        BrandLogo(String name, Integer thresh) {
            this.logo_name = name;
            this.threshold = thresh;
        }
    }

    private static final String TAG = "demon-go-brand-det";
    FeatureDetector Orbdetector;
    DescriptorExtractor OrbExtractor;
    DescriptorMatcher matcher;


    private Context context;
    private long lastNotificationTimestamp = System.currentTimeMillis();
    private final String NOTIFICATION_CHANNEL_ID = "demon-go-notifications";
    private int notificationId = 0;
    private RequestQueue requestQueue;

    private HashMap<String, ArrayList<BrandLogo>> objectTemplateNameMap = new HashMap<String, ArrayList<BrandLogo>>(){{
        put("mate", new ArrayList<BrandLogo>(){{
            add(new BrandLogo("mate_logo", 25));
            add(new BrandLogo("mate_label", 25));
            add(new BrandLogo("mate_flasche", 25));
            add(new BrandLogo("club_mate_logo_x25", 25));
        }});
        put("ahoj_brause", new ArrayList<BrandLogo>(){{
            add(new BrandLogo("ahoj_brause_logo", 25));
        }});
        put("thinkpad", new ArrayList<BrandLogo>(){{
            add(new BrandLogo("thinkpad-logo-white", 25));
        }});
    }};

    public BrandDetectionStep(Context context, RequestQueue requestQueue){
        this.context = context;
        this.requestQueue = requestQueue;
        createNotificationChannel();

        //needed for feature matching
        Orbdetector = FeatureDetector.create(FeatureDetector.ORB);

        //Orbdetector.read(outputFile.getPath());
        OrbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        //read the template images and save them in the local template list
        InputStream stream = null;
        for (String object : objectTemplateNameMap.keySet()){
            for (BrandDetectionStep.BrandLogo logo : this.objectTemplateNameMap.get(object)) {
                Mat templ;
                MatOfKeyPoint keypointsTemplate = new MatOfKeyPoint();
                Mat descriptorsTemplate = new Mat();

                Uri uri = Uri.parse(logo.logo_name + ".png");

                try {
                    stream = context.getAssets().open(uri.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
                bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bmp = BitmapFactory.decodeStream(stream, null, bmpFactoryOptions);

                templ = new Mat();
                Utils.bitmapToMat(bmp, templ);

                //get the keypoints of the template
                Orbdetector.detect(templ, keypointsTemplate);
                OrbExtractor.compute(templ, keypointsTemplate, descriptorsTemplate);

                final Template template = new Template(templ, keypointsTemplate, descriptorsTemplate, logo.logo_name);
                logo.template = template;
            }
        }
    }


    //needed if you want to experiment with different ORBFeatureDetector settings
    private void writeToFile(File file, String data) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /*
     * checks if for a given object with multiple images one match can be found
    */
    private boolean matchFeatures(Mat frame){

        Mat descriptorsImg = new Mat();
        MatOfKeyPoint keypointsImg = new MatOfKeyPoint();
        //feature detection is very expensive: takes about 100 times as long as the matching (~0.25s)
        Orbdetector.detect(frame, keypointsImg);
        OrbExtractor.compute(frame, keypointsImg, descriptorsImg);

        MatOfDMatch matches = new MatOfDMatch();
        for (String objectName : this.objectTemplateNameMap.keySet()){

            for (BrandDetectionStep.BrandLogo logo : this.objectTemplateNameMap.get(objectName)) {

                matcher.match(descriptorsImg, logo.template.descriptors, matches);

                List<DMatch> matchesList = matches.toList();

                for (int i = 0; i < descriptorsImg.rows(); i++) {
                    if (matchesList.get(i).distance < logo.threshold) {
                        Log.i(TAG, "match found " + logo.template.uri + " Distance: " + matchesList.get(i).distance);
                        if (System.currentTimeMillis() - lastNotificationTimestamp > 3000){
                            sendNotification(objectName);
                            lastNotificationTimestamp = System.currentTimeMillis();
                        }
                        this.sendDetectedBrand(objectName);

                        return true;
                    }
                }
            }

        }

        return false;
    }


    @Override
    public void process(Snapshot last) {
         if(matchFeatures(last.mat)){
             this.output(last);
         }
    }

    private void sendNotification(String object){
        String content = "";
        if (object.equals("mate")){
            content = "Flora Mate. Erfrischt und belebt";
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Need some refreshment?")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId++, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "demon-go-brand-recognition";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendDetectedBrand(final String brand_name) {
        String url = "http://139.59.145.241:5000/brand";
        Log.i(TAG, "sendDetectedBrand");

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "sendDetectedBrand: " + error);

                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null) {
                    Log.e(TAG, "sendDetectedBrand: " + String.valueOf(networkResponse.statusCode));
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("brand", brand_name);
                return params;
            }
        };

        this.requestQueue.add(request);
    }

    class Template {
        Mat template;
        MatOfKeyPoint keypoints;
        Mat descriptors;
        String uri;

        public Template(Mat template, MatOfKeyPoint keypoints, Mat descriptors, String uri) {
            this.template = template;
            this.keypoints = keypoints;
            this.descriptors = descriptors;
            this.uri = uri;
        }
    }
}
