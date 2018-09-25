package com.github.demongo;

import android.util.Log;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import hpi.gitlab.demongo.pipeline.Snapshot;
import hpi.gitlab.demongo.pipeline.StepWithQueue;

public class AngleChangeStep extends StepWithQueue {

    private boolean hasNewAngle = false;
    private Matrix4 lastPictureTransform = null;

    AngleChangeStep() {
    }

    @Override
    public void process(Snapshot last) {
        queue(last);

        if (hasNewAngle) {
            Log.e("demon-go-angle", "angle change output");
            this.output(getBestAndClear());
            hasNewAngle = false;
        }
    }

    boolean checkPictureTransformDelta(Matrix4 current) {
        if (lastPictureTransform == null)
            lastPictureTransform = current;

        Vector3 lastPosition = new Vector3();
        Vector3 currentPosition = new Vector3();

        current.getTranslation(currentPosition);
        lastPictureTransform.getTranslation(lastPosition);

        Quaternion lastRotation = new Quaternion();
        Quaternion currentRotation = new Quaternion();

        current.getRotation(currentRotation);
        lastPictureTransform.getRotation(lastRotation);

        float lastAngle = lastRotation.getAngle();
        float currentAngle = currentRotation.getAngle();

        if (lastPosition.dst(currentPosition) > 0.1 || Math.abs(lastAngle - currentAngle) > 5) {
            lastPictureTransform.set(current);
            hasNewAngle = true;
            return true;
        }
        return false;
    }
}
