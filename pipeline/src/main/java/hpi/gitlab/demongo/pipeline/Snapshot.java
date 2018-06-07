package hpi.gitlab.demongo.pipeline;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class Snapshot {
    protected Mat mat;
    protected double score;
    private Mat debugMat;

    public Snapshot(Mat mat, double score, Mat debugMat) {
        this.mat = mat;
        this.score = score;
        this.debugMat = debugMat;
    }

    public Snapshot(Mat mat, double score) {
        this(mat, score, null);
    }

    public Snapshot copyWith(Mat mat, double score) {
        return new Snapshot(mat, score, debugMat);
    }

    public Snapshot copyWithNewMat(Mat newMat) {
        return new Snapshot(newMat, score, debugMat);
    }

    public Snapshot copyWithNewScore(double newScore) {
        return new Snapshot(mat, newScore, debugMat);
    }

    public Mat createDebugMat() {
        debugMat = mat.clone();
        return debugMat;
    }

    public Mat getDebugMat() {
        return debugMat;
    }

    public boolean isDebug() {
        return (getDebugMat() != null);
    }

    private String matToBase64String(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 92, stream);
        byte[] imageBytes = stream.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    public Map<String, String> getRequestParameterList() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("image", matToBase64String(this.mat));
//        parameters.put("score", Double.toString(this.score));
        return parameters;
    }
}
