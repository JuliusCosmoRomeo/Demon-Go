package hpi.gitlab.demongo.pipeline;

import android.graphics.Bitmap;
import android.util.Base64;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Snapshot {
    protected Mat mat;
    protected double score;
    private Mat debugMat;
    public int x_offset;
    public int y_offset;

    public Snapshot(Mat mat, double score, Mat debugMat, int x_off, int y_off) {
        this.mat = mat;
        this.score = score;
        this.debugMat = debugMat;
        this.x_offset = x_off;
        this.y_offset = y_off;
    }

    public Snapshot(Mat mat, double score) {
        this(mat, score, null, 0, 0);
    }

    public Snapshot copyWith(Mat mat, double score) {
        return new Snapshot(mat, score, debugMat, x_offset, y_offset);
    }

    public Snapshot copyWithNewMat(Mat newMat, int x_off, int y_off) {
        return new Snapshot(newMat, score, debugMat, x_off, y_off);
    }

    public Snapshot copyWithNewScore(double newScore) {
        return new Snapshot(mat, newScore, debugMat, x_offset, y_offset);
    }

    public Mat createDebugMat() {
        debugMat = mat.clone();
        return debugMat;
    }

    public void setDebugMat(Mat newMat) {
        debugMat = newMat.clone();
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
//        parameters.put("token", FirebaseInstanceId.getInstance().getToken());
        // parameters.put("score", Double.toString(this.score));
        return parameters;
    }

    public ArrayList<Float> processServerResponse(float x, float y) {
        return null;
    }
}
