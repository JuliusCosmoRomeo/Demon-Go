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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import hpi.gitlab.demongo.pipeline.Snapshot;

public class ARSnapshot extends Snapshot {

    private float[] viewProjectionMatrix = new float[16];
    private float[] points;

    ARSnapshot(double score, Frame frame) throws NotYetAvailableException {
        super(matFromFrame(frame), score);

        PointCloud c = frame.acquirePointCloud();
        FloatBuffer cloud = c.getPoints();
        int num = 0;
        points = new float[cloud.limit() - cloud.limit() / 4];
        while (cloud.hasRemaining()) {
            points[num++] = cloud.get();
            points[num++] = cloud.get();
            points[num++] = cloud.get();
            float confidence = cloud.get();
        }
        c.release();

        getViewProjectionMatrix(frame.getCamera());
    }

    private static Mat matFromFrame(Frame frame) throws NotYetAvailableException {
        Image image = frame.acquireCameraImage();
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
        byte[] pixels = new byte[buffer.limit()];
        buffer.position(0);
        buffer.get(pixels);
        mat.put(0, 0, pixels);
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
                Log.e("demon-go-ar", point.toString());
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
}
