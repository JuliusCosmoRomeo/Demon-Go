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
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class TemplateMatching {


    Mat templ;
    MatOfKeyPoint keypointsTemplate = new MatOfKeyPoint();
    Mat descriptorsTemplate = new Mat();
    FeatureDetector Orbdetector;
    DescriptorExtractor OrbExtractor;
    DescriptorMatcher matcher;



    public TemplateMatching(Context context){

        InputStream stream = null;
        Uri uri = Uri.parse("android.resource://com.github.demongo.opencv/drawable/template");
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

        //needed for feature matching
        Orbdetector = FeatureDetector.create(FeatureDetector.ORB);
        OrbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        //get the keypoints of the template
        Orbdetector.detect(templ, keypointsTemplate);
        OrbExtractor.compute(templ, keypointsTemplate, descriptorsTemplate);
    }

    public Mat matchFeatures(Mat img){
        //TODO: try with grayscale
        Mat descriptorsImg = new Mat();
        MatOfKeyPoint keypointsImg = new MatOfKeyPoint();
        Orbdetector.detect(img, keypointsImg);
        OrbExtractor.compute(img, keypointsImg, descriptorsImg);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsImg,descriptorsTemplate,matches);

        List<DMatch> matchesList = matches.toList();
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();

        Collections.sort(matchesList,  new Comparator<DMatch>() {
            @Override
            public int compare(DMatch a, DMatch b) {
                return (int)(a.distance - b.distance);
            }
        });
        MatOfDMatch gm = new MatOfDMatch();
        for (int i=0;i<descriptorsImg.rows();i++){
            if(matchesList.get(i).distance<25){ // 3*min_dist is my threshold here
                good_matches.addLast(matchesList.get(i));

            }
        }
        gm.fromList(good_matches);
        if(!good_matches.isEmpty()){

            for (DMatch match : good_matches){
                Log.d("demon-go", "distance " + match.distance);
            }

        Log.d("demon-go", "matches  " + gm.toList().size());
        }

        //Mat imageOut = img.clone();
        //Features2d.drawMatches(img, keypointsImg, templ, keypointsTemplate, gm, imageOut);
        //return imageOut;
        return img;


    }


    public Mat findTemplate(Mat img) {

        int match_method = Imgproc.TM_CCOEFF;
        // / Create the result matrix
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

        return img;
    }
}