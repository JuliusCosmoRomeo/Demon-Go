package com.github.demongo;

import android.opengl.Matrix;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.github.demongo.DemonParticles;
import com.github.demongo.Shot;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Point;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

public class ARDemon {

    interface PhaseChangedListener {
        void changed(ARDemon demon, Phase phase);
    }

    enum Phase {
        SCANNING,
        CAPTURING,
        CAPTURED
    }

    enum MovementMode {
        IN_ROOM,
        FIXED_BOX
    }

    private static final float DEMON_SCALE = 0.6f;

    private static MovementMode MOVEMENT_MODE = MovementMode.IN_ROOM;

    private static final int MIN_TARGETS = 3;
    private static final int MAX_TARGETS = 5;

    private static final float SPEED = 2;
    private static final float SPHERE_SIZE = 0.8f;
    private static final String MODEL_PATH = "demon01.g3db";

    private ModelInstance instance;
    private Model demonModel;
    private Vector3 position = new Vector3();
    private boolean waitingAtTarget = false;

    private ArrayList<Shot> shots = new ArrayList<>();

    private DemonParticles particles;

    private Phase phase = Phase.SCANNING;

    private PhaseChangedListener phaseChangedListener;

    ARDemon(Camera camera, AssetManager assetManager, PhaseChangedListener listener) {
        phaseChangedListener = listener;

        assetManager.load(MODEL_PATH, Model.class);
        assetManager.finishLoading();

        particles = new DemonParticles(camera);

        ModelBuilder modelBuilder = new ModelBuilder();
        Model sphereModel = modelBuilder.createSphere(SPHERE_SIZE, SPHERE_SIZE, SPHERE_SIZE, 10, 10,
                new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        demonModel = assetManager.get(MODEL_PATH, Model.class);
        demonModel.meshes.get(0).scale(DEMON_SCALE, DEMON_SCALE, DEMON_SCALE);

        instance = new ModelInstance(sphereModel);
    }

    private static float easeInOutQuad(float t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private Vector3 randomLastPosition = new Vector3();
    private Vector3 randomTargetPosition = new Vector3();
    private float interpTime = 0.0f;

    private static float MIN_ROOM_SIZE = 2.0f;
    private static float MAX_ROOM_SIZE = 10.0f;
    private Vector3 roomMin = new Vector3(-MIN_ROOM_SIZE, 0, -MIN_ROOM_SIZE);
    private Vector3 roomMax = new Vector3(MIN_ROOM_SIZE, MIN_ROOM_SIZE, MIN_ROOM_SIZE);

    private Anchor[] anchors;
    private int currentTarget = 0;

    private void setPositionTowards(Vector3 position, Vector3 target) {
        Quaternion currentRotation = new Quaternion();
        Quaternion desiredRotation = new Quaternion();

        instance.transform.getRotation(currentRotation, true);
        new Matrix4().setToLookAt(position, target, new Vector3(0, 1, 0)).getRotation(desiredRotation, true);

        instance.transform.set(currentRotation.slerp(desiredRotation, Gdx.graphics.getDeltaTime()));
        instance.transform.setTranslation(position);
    }

    private static Vector3 anchorToTranslation(Anchor a) {
        Pose p = a.getPose();
        return new Vector3(p.tx(), p.ty(), p.tz());
    }

    private Vector3 randomPointInRoom(float maxHeight) {
        if (MOVEMENT_MODE == MovementMode.FIXED_BOX) {
            final float X_Z_METERS = 4.0f;
            final float Y_TO_TOP_METERS = 1.5f;
            final float Y_TO_BOTTOM_METERS = 1.0f;
            return new Vector3(((float) Math.random()) * X_Z_METERS * 2 - X_Z_METERS,
                    Math.min(((float) Math.random()) * X_Z_METERS * 2 - X_Z_METERS, maxHeight),
                    ((float) Math.random()) * (Y_TO_BOTTOM_METERS + Y_TO_TOP_METERS) - Y_TO_BOTTOM_METERS);
        } else {
            return new Vector3(lerp(roomMin.x, roomMax.x, (float) Math.random()),
                    Math.min(maxHeight, lerp(roomMin.y, roomMax.y, (float) Math.random())),
                    lerp(roomMin.z, roomMax.z, (float) Math.random()));
        }
    }

    public void move(Vector3 currentRoomMin, Vector3 currentRoomMax, Vector3 cameraPosition) {
        if (currentRoomMin != null && currentRoomMax != null) {
            roomMin.set(Math.min(roomMin.x, currentRoomMin.x), Math.min(roomMin.y, currentRoomMin.y), Math.min(roomMin.z, currentRoomMin.z));
            roomMax.set(Math.max(roomMax.x, currentRoomMax.x), Math.max(roomMax.y, currentRoomMax.y), Math.max(roomMax.z, currentRoomMax.z));
            roomMin.clamp(-MIN_ROOM_SIZE, -MAX_ROOM_SIZE);
            roomMax.clamp(MIN_ROOM_SIZE, MAX_ROOM_SIZE);
        }

        if (phase == Phase.SCANNING) {
            interpTime += Gdx.graphics.getDeltaTime() * 0.3;
            if (interpTime >= 1.0f) {
                interpTime = 0.0f;
                randomLastPosition.set(randomTargetPosition);
                randomTargetPosition.set(randomPointInRoom(3));
            }
            position.set(randomLastPosition);
            position.lerp(randomTargetPosition, easeInOutQuad(interpTime));
            instance.transform.setTranslation(position);
        } else if (phase == Phase.CAPTURING) {
            if (!waitingAtTarget) {
                Vector3 delta = anchorToTranslation(anchors[currentTarget]).sub(position);
                float distance = delta.len();
                position.add(delta.nor().scl(Gdx.graphics.getDeltaTime() * SPEED * Math.min(1, distance)));
                setPositionTowards(position, anchorToTranslation(anchors[currentTarget]));

                if (distance < 0.05) {
                    waitingAtTarget = true;
                    // TODO start animation
                }
            } else {
                setPositionTowards(anchorToTranslation(anchors[currentTarget]), cameraPosition);
            }
        }
    }

    public boolean moveToNextTarget() {
        waitingAtTarget = false;
        currentTarget++;
        return currentTarget < anchors.length;
    }

    /**
     * Take the provided targets and make sure we got at least MIN_TARGETS and use
     * at most MAX_TARGETS
     *
     * @param t list of target vector positions
     */
    public void setTargets(Vector3[] t, Session session) {
        Vector3[] targets = new Vector3[Math.min(Math.max(t.length, MIN_TARGETS), MAX_TARGETS)];
        if (t.length < MIN_TARGETS) {
            System.arraycopy(t, 0, targets, 0, t.length);
            for (int i = t.length; i < MIN_TARGETS; i++) {
                Vector3 point = randomPointInRoom(1.5f);
                targets[i] = point;
            }
        } else if (t.length > MAX_TARGETS) {
            System.arraycopy(t, 0, targets, 0, MAX_TARGETS);
        }

        anchors = new Anchor[targets.length];
        for (int i = 0; i < targets.length; i++) {
            Vector3 point = targets[i];
            anchors[i] = session.createAnchor(Pose.makeTranslation(point.x, point.y, point.z));
        }
    }

    Anchor[] getAnchors() {
        return anchors;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        if (phase == Phase.CAPTURED)
            return;

        particles.setPositionFrom(instance.transform);
        particles.draw(modelBatch);
        modelBatch.render(instance, environment);

        for (Shot shot : shots) {
            shot.draw(modelBatch, environment);
        }
    }

    public Vector3 getCurrentTarget() {
        if (phase != Phase.CAPTURING)
            return Vector3.Zero;
        return anchorToTranslation(anchors[currentTarget]);
    }

    public Vector3 getPosition() {
        return position;
    }

    public void shoot(Ray ray) {
        if (phase != Phase.SCANNING)
            return;

        shots.add(new Shot(ray.cpy()));

        if (hitByRay(ray)) {
            enterCapturingPhase();
        }
    }

    private boolean hitByRay(Ray ray) {
        Vector3 intersection = new Vector3();
        return Intersector.intersectRaySphere(ray, position, SPHERE_SIZE / 2, intersection);
    }

    private void enterCapturingPhase() {
        if (phase == Phase.CAPTURING) {
            return;
        }

        phase = Phase.CAPTURING;
        instance = new ModelInstance(demonModel);
        phaseChangedListener.changed(this, getPhase());
    }

    public void setCaptured() {
        if (phase == Phase.CAPTURED) {
            return;
        }

        phase = Phase.CAPTURED;
        phaseChangedListener.changed(this, getPhase());
    }

    // accessors for debug info
    public Vector3 getRoomMin() { return roomMin; }
    public Vector3 getRoomMax() { return roomMax; }
    public Phase getPhase() { return phase; }
}