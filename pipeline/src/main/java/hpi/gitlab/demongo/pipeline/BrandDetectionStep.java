package hpi.gitlab.demongo.pipeline;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BrandDetectionStep extends Step {

    private static final String TAG = "demon-go-brand-det";
    FeatureDetector Orbdetector;
    DescriptorExtractor OrbExtractor;
    DescriptorMatcher matcher;
    //all templates go into this map
    HashMap<String, ArrayList<Template>> objectTemplateMap = new HashMap();


    private Context context;
    private long lastNotificationTimestamp = System.currentTimeMillis();
    private final String NOTIFICATION_CHANNEL_ID = "demon-go-notifications";
    private int notificationId = 0;

    public BrandDetectionStep(Context context, HashMap<String, ArrayList<String>> templatesMap){
        this.context = context;
        createNotificationChannel();

        //needed for feature matching
        Orbdetector = FeatureDetector.create(FeatureDetector.ORB);

        /*String orbParamFileName = "/path/to/file/orb_params.yml";
        File outputDir = context.getCacheDir(); // If in an Activity (otherwise getActivity.getCacheDir();
        File outputFile = null;
        try {
            outputFile = File.createTempFile("orbDetectorParams", ".YAML", outputDir);
            writeToFile(outputFile, "%YAML:1.0\nscoreType: 1\nedgeThreshold: 1\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        //Orbdetector.read(outputFile.getPath());
        OrbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        //read the template images and save them in the local template list
        InputStream stream = null;
        for (String object : templatesMap.keySet()){
            for (String templateString : templatesMap.get(object)) {
                Mat templ;
                MatOfKeyPoint keypointsTemplate = new MatOfKeyPoint();
                Mat descriptorsTemplate = new Mat();

                Uri uri = Uri.parse(templateString + ".png");

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

                //add the template to the objectTemplateMap
                final Template template = new Template(templ, keypointsTemplate, descriptorsTemplate, templateString);
                ArrayList<Template> templateListForObject = objectTemplateMap.get(object);
                if (templateListForObject == null) {
                    templateListForObject = new ArrayList<Template>() {{
                        add(template);
                    }};
                } else {
                    templateListForObject.add(template);
                }
                objectTemplateMap.put(object, templateListForObject);
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
        for (String objectName : objectTemplateMap.keySet()){

            for (Template templ : objectTemplateMap.get(objectName)) {

                matcher.match(descriptorsImg, templ.descriptors, matches);

                List<DMatch> matchesList = matches.toList();

                for (int i = 0; i < descriptorsImg.rows(); i++) {
                    //TODO: find a better threshold abstraction
                    if (matchesList.get(i).distance < 25) {
                        Log.i(TAG, "match found " + templ.uri);
                        if (System.currentTimeMillis() - lastNotificationTimestamp > 3000){
                            sendNotification(objectName);
                            lastNotificationTimestamp = System.currentTimeMillis();
                        }


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
            content = "Try out Flora Mate now. Only 0.2 g sugar more than your Club Mate";
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Still thirsty?")
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
            int importance = NotificationManager.IMPORTANCE_MAX;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
