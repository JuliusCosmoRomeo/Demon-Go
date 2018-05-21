package com.github.demongo.opencv;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class TemplateMatching {


    FeatureDetector Orbdetector;
    DescriptorExtractor OrbExtractor;
    DescriptorMatcher matcher;
    //all templates go into this list
    ArrayList<Template> templateList = new ArrayList<>();


    public TemplateMatching(Context context, ArrayList<String> templates){

        //needed for feature matching
        Orbdetector = FeatureDetector.create(FeatureDetector.ORB);
        OrbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        InputStream stream = null;
        String baseURI = "android.resource://com.github.demongo.opencv/drawable/";
        for (String template : templates){
            Mat templ;
            MatOfKeyPoint keypointsTemplate = new MatOfKeyPoint();
            Mat descriptorsTemplate = new Mat();

            Uri uri = Uri.parse(baseURI + template);
            try {
                stream = context.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
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
            templateList.add(new Template(templ,keypointsTemplate,descriptorsTemplate,template));
        }


    }



    public Mat matchFeatures(Mat img){
        //TODO: try with grayscale
        Mat descriptorsImg = new Mat();
        MatOfKeyPoint keypointsImg = new MatOfKeyPoint();
        Orbdetector.detect(img, keypointsImg);
        OrbExtractor.compute(img, keypointsImg, descriptorsImg);
        MatOfDMatch matches = new MatOfDMatch();
        for (Template templ : templateList){
            matcher.match(descriptorsImg,templ.descriptors,matches);

            List<DMatch> matchesList = matches.toList();
            LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
            /*
            * for sorting

            Collections.sort(matchesList,  new Comparator<DMatch>() {
                @Override
                public int compare(DMatch a, DMatch b) {
                    return (int)(a.distance - b.distance);
                }
            });
            */
            MatOfDMatch gm = new MatOfDMatch();
            for (int i=0;i<descriptorsImg.rows();i++){
                if(matchesList.get(i).distance<25){ // 3*min_dist is my threshold here
                    good_matches.addLast(matchesList.get(i));

                }
            }
            gm.fromList(good_matches);
            if(!good_matches.isEmpty()){

                for (DMatch match : good_matches){
                    Log.d("demon-go", "match: " + templ.uri + " distance " + match.distance);
                }

            }

            //we need to convert our Mat to RGB color scheme because drawKeypoints() doesn't work with RGBA


            //Mat rgbImg = new Mat();
            //this is needed if you want to try drawMatches() -> unfortunately doesn't work yet
            //Mat rgbTempl = new Mat();
            //Mat imgOut = new Mat(img.cols() + templ.cols(),img.rows()+templ.rows(),img.type());
            //imgOut.reshape(img.cols(),img.rows());
            //Features2d.drawMatches(rgbImg, keypointsImg, rgbTempl,keypointsTemplate,gm, rgbImg, new Scalar(100,100,200),new Scalar(150,50,50),new MatOfByte(),Features2d.DRAW_OVER_OUTIMG);
            //Imgproc.cvtColor(templ, rgbTempl, Imgproc.COLOR_RGBA2RGB);
            //templ.copyTo(img.submat(new Rect(0, 0, templ.cols(), templ.rows())));

        }

        Mat rgbImg = new Mat();
        Imgproc.cvtColor(img, rgbImg, Imgproc.COLOR_RGBA2RGB);
        Features2d.drawKeypoints(rgbImg, keypointsImg, rgbImg);
        Imgproc.cvtColor(rgbImg, img, Imgproc.COLOR_RGB2RGBA);


        return img;


    }


    public Mat findTemplate(Mat img) {

        int match_method = Imgproc.TM_CCOEFF;
        // / Create the result matrix
        for (Template template : templateList){

            Mat templ = template.template;
            int result_cols = img.cols() - templ.cols() + 1;
            int result_rows = img.rows() - templ.rows() + 1;
            Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

            // / Do the Matching and Normalize
            Imgproc.matchTemplate(img, templ, result, match_method);
            Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

            // / Localizing the best match with minMaxLoc
            MinMaxLocResult mmr = Core.minMaxLoc(result);
            Point matchLoc;
            Log.d("demon-go", "max similarity value " + mmr.maxVal);
            Log.d("demon-go", "min similarity value " + mmr.minVal);

            if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
                matchLoc = mmr.minLoc;
            } else {
                matchLoc = mmr.maxLoc;
            }

            // / Show me what you got
            Imgproc.rectangle(img, matchLoc, new Point(matchLoc.x + templ.cols(),
                    matchLoc.y + templ.rows()), new Scalar(0, 255, 0));
        }

        return img;
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