package hpi.gitlab.demongo.pipeline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

    public BrandDetectionStep(Context context, HashMap<String, ArrayList<String>> templatesMap){
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
                ArrayList<Template> templateListForObject = new ArrayList<>();
                if (templatesMap.get(object) == null) {
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
