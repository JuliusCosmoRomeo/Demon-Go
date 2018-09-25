package com.github.demongo;

import android.media.Image;
import android.opengl.Matrix;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import hpi.gitlab.demongo.pipeline.Snapshot;

public class ARSnapshot extends Snapshot {
    private static final String TAG = "demon-go-ARSnapshot";

    // code from http://answers.opencv.org/question/61628/android-camera2-yuv-to-rgb-conversion-turns-out-green/
    private static Mat convertYuv420888ToMat(Image image, boolean isGreyOnly) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane yPlane = image.getPlanes()[0];
        int ySize = yPlane.getBuffer().remaining();

        if (isGreyOnly) {
            byte[] data = new byte[ySize];
            yPlane.getBuffer().get(data, 0, ySize);

            Mat greyMat = new Mat(height, width, CvType.CV_8UC1);
            greyMat.put(0, 0, data);

            return greyMat;
        }

        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        // be aware that this size does not include the padding at the end, if there is any
        // (e.g. if pixel stride is 2 the size is ySize / 2 - 1)
        int uSize = uPlane.getBuffer().remaining();
        int vSize = vPlane.getBuffer().remaining();

        byte[] data = new byte[ySize + (ySize/2)];

        yPlane.getBuffer().get(data, 0, ySize);

        ByteBuffer ub = uPlane.getBuffer();
        ByteBuffer vb = vPlane.getBuffer();

        int uvPixelStride = uPlane.getPixelStride(); //stride guaranteed to be the same for u and v planes
        if (uvPixelStride == 1) {
            uPlane.getBuffer().get(data, ySize, uSize);
            vPlane.getBuffer().get(data, ySize + uSize, vSize);

            Mat yuvMat = new Mat(height + (height / 2), width, CvType.CV_8UC1);
            yuvMat.put(0, 0, data);
            Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
            Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_I420, 3);
            yuvMat.release();
            return rgbMat;
        }

        // if pixel stride is 2 there is padding between each pixel
        // converting it to NV21 by filling the gaps of the v plane with the u values
        vb.get(data, ySize, vSize);
        for (int i = 0; i < uSize; i += 2) {
            data[ySize + i + 1] = ub.get(i);
        }

        Mat yuvMat = new Mat(height + (height / 2), width, CvType.CV_8UC1);
        yuvMat.put(0, 0, data);
        Mat rgbMat = new Mat(height, width, CvType.CV_8UC3);
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3);
        yuvMat.release();
        return rgbMat;
    }

    private float[] viewProjectionMatrix = new float[16];
    private float[] points;

    Vector3 max = new Vector3();
    Vector3 min = new Vector3();

    private ARSnapshot(Mat mat, double score, float[] points, float[] viewProjectionMatrix, Mat debugMat, int x_off, int y_off) {
        super(mat, score, debugMat, x_off, y_off);
        this.points = points;
        this.viewProjectionMatrix = viewProjectionMatrix;
    }

    ARSnapshot(double score, Frame frame) throws NotYetAvailableException {
        super(matFromFrame(frame), score);

        PointCloud c = frame.acquirePointCloud();
        getPointCloudCopy(c);

        getViewProjectionMatrix(frame.getCamera());
    }

    private void getPointCloudCopy(PointCloud c) {
        FloatBuffer cloud = c.getPoints();
        int num = 0;
        points = new float[cloud.limit() - cloud.limit() / 4];
        while (cloud.hasRemaining()) {
            float x = cloud.get();
            float y = cloud.get();
            float z = cloud.get();
            if (x < min.x) min.x = x; else if (x > max.x) max.x = x;
            if (y < min.y) min.y = y; else if (y > max.y) max.y = y;
            if (z < min.z) min.z = z; else if (z > max.z) max.z = z;
            points[num++] = x;
            points[num++] = y;
            points[num++] = z;
            cloud.get(); // confidence
        }
        c.release();
    }

    private static Mat matFromFrame(Frame frame) throws NotYetAvailableException {
        Image image = frame.acquireCameraImage();
        Mat mat = convertYuv420888ToMat(image, false);
        image.close();
        return mat;
    }

    private void getViewProjectionMatrix(Camera camera) {
        float[] projectionMatrix = new float[16];
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

        float[] viewMatrix = new float[16];
        camera.getViewMatrix(viewMatrix, 0);

        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public Vector3 projectPoint(float x, float y) {
        Vector3 point = new Vector3();
        int viewportWidth = Gdx.graphics.getWidth();
        int viewportHeight = Gdx.graphics.getHeight();
        y = viewportHeight - y;
        Matrix4 transform = new Matrix4(viewProjectionMatrix);

        float minDistance = 99999999999.0f;
        Vector3 best = new Vector3();
        Log.e(TAG, "projectPoint: " + points.length);

        for (int i = 0; i < points.length;) {
            point.set(points[i++], points[i++], points[i++]);
            point.prj(transform);
            point.x = viewportWidth * (point.x + 1) / 2;
            point.y = viewportHeight * (point.y + 1) / 2;
            point.z = (point.z + 1) / 2;

            if (point.x < 0 || point.y < 0 || point.x > viewportWidth || point.y > viewportHeight)
                continue;

            float a = x - point.x;
            float b = y - point.y;
            float distance = a * a + b * b;
            if (distance < minDistance) {
                minDistance = distance;
                best.set(points[i - 3], points[i - 2], points[i - 1]);
                Log.e(TAG, point.toString());
            }
        }

        return best;
    }

    public Vector3 projectPoint2(float x, float y) {
        Ray ray = screenPointToRay(x, y, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), viewProjectionMatrix);
        return closestPoint(ray);
    }

    private Vector3 closestPoint(Ray ray) {
        final float MAX_DISTANCE = 10.0f;

        Vector3 point = new Vector3();
        Vector3 sum = new Vector3();
        for (int i = 0; i < points.length;) {
            point.set(points[i++], points[i++], points[i++]);

            if (point.dot(ray.direction) < 0)
                continue;

            float d = distanceRayPoint(ray, point);
            sum.add(point.scl(d / MAX_DISTANCE));
        }
        return sum;
    }

    private float distanceRayPoint(Ray ray, Vector3 point) {
        Vector3 p0 = ray.origin;
        Vector3 p1 = new Vector3(ray.origin).add(new Vector3(ray.direction).scl(10));

        Vector3 d = new Vector3(p1).sub(p0).scl(1.0f / p0.dst(p1));
        Vector3 v = new Vector3(p0).sub(p1);
        float t = v.dot(d);
        Vector3 p = new Vector3(p0).add(d.scl(t));
        return p.dst(p0);
    }

    private static Ray screenPointToRay(float vx, float vy, float viewportWidth, float viewportHeight, float[] viewProjMtx)
    {
        vy = viewportHeight - vy;
        float x = vx * 2.0F / viewportWidth - 1.0F;
        float y = vy * 2.0F / viewportHeight - 1.0F;

        float[] farScreenPoint = { x, y, 1.0F, 1.0F };

        float[] nearScreenPoint = { x, y, -1.0F, 1.0F };

        float[] nearPlanePoint = new float[4];
        float[] farPlanePoint = new float[4];

        float[] invertedProjectionMatrix = new float[16];
        Matrix.setIdentityM(invertedProjectionMatrix, 0);
        Matrix.invertM(invertedProjectionMatrix, 0, viewProjMtx, 0);
        Matrix.multiplyMV(nearPlanePoint, 0, invertedProjectionMatrix, 0, nearScreenPoint, 0);
        Matrix.multiplyMV(farPlanePoint, 0, invertedProjectionMatrix, 0, farScreenPoint, 0);

        Vector3 direction = new Vector3(farPlanePoint[0] / farPlanePoint[3], farPlanePoint[1] / farPlanePoint[3], farPlanePoint[2] / farPlanePoint[3]);

        Vector3 origin = new Vector3(new Vector3(nearPlanePoint[0] / nearPlanePoint[3], nearPlanePoint[1] / nearPlanePoint[3], nearPlanePoint[2] / nearPlanePoint[3]));

        direction.sub(origin);
        direction.nor();

        return new Ray(origin, direction);
    }

    public ARSnapshot copyWith(Mat newMat, double newScore) {
        return new ARSnapshot(newMat, newScore, points, viewProjectionMatrix, getDebugMat(), x_offset, y_offset);
    }

    public ARSnapshot copyWithNewMat(Mat newMat, int x_off, int y_off) {
        return new ARSnapshot(newMat, score, points, viewProjectionMatrix, getDebugMat(), x_off, y_off);
    }

    public ARSnapshot copyWithNewScore(double newScore) {
        return new ARSnapshot(mat, newScore, points, viewProjectionMatrix, getDebugMat(), x_offset, y_offset);
    }

    @Override
    public ArrayList<Float> processServerResponse(float x, float y) {
        super.processServerResponse(x, y);
        Vector3 best = this.projectPoint(x, y);
        if (best.x != 0.0 && best.y != 0.0 && best.z != 0.0) {
            ArrayList<Float> targetCoordinates = new ArrayList<Float>();
            targetCoordinates.add(best.x);
            targetCoordinates.add(best.y);
            targetCoordinates.add(best.z);
            return targetCoordinates;
        } else {
            Log.i(TAG, "processServerResponse: Ommitting projection to 0;0;0");
            return null;
        }
    }
}
