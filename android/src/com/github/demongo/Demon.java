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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import java.util.ArrayList;

public class Demon {

    enum Phase {
        SCANNING,
        CAPTURING
    }

    enum MovementMode {
        IN_ROOM,
        FIXED_BOX
    }

    private static MovementMode MOVEMENT_MODE = MovementMode.IN_ROOM;

    private Vector3 target = new Vector3();

    private static final float SPEED = 30;
    private static final float SPHERE_SIZE = 0.8f;
    private static final String MODEL_PATH = "demon01.g3db";

    private ModelInstance instance;
    private Model demonModel;
    private Vector3 velocity = new Vector3();
    private Vector3 position = new Vector3();

    private ArrayList<Shot> shots = new ArrayList<>();

    private DemonParticles particles;

    private Phase phase = Phase.SCANNING;

    Demon(Camera camera, AssetManager assetManager) {
        assetManager.load(MODEL_PATH, Model.class);
        assetManager.finishLoading();

        particles = new DemonParticles(camera);

        ModelBuilder modelBuilder = new ModelBuilder();
        Model sphereModel = modelBuilder.createSphere(SPHERE_SIZE, SPHERE_SIZE, SPHERE_SIZE, 10,10,
                new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        demonModel = assetManager.get(MODEL_PATH, Model.class);

        instance = new ModelInstance(sphereModel);
    }


    private static float easeInOutQuad(float t) {
        return t<.5 ? 2*t*t : -1+(4-2*t)*t;
    }

    private Vector3 randomLastPosition = new Vector3();
    private Vector3 randomTargetPosition = new Vector3();
    private Vector3 randomCurrentPosition = new Vector3();
    private float interpTime = 0.0f;

    private static float MIN_ROOM_SIZE = 2.0f;
    private static float MAX_ROOM_SIZE = 10.0f;
    private Vector3 roomMin = new Vector3(-MIN_ROOM_SIZE, 0, -MIN_ROOM_SIZE);
    private Vector3 roomMax = new Vector3(MIN_ROOM_SIZE, MIN_ROOM_SIZE, MIN_ROOM_SIZE);

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    Vector3[] targets = {
        new Vector3(5, 1, -2),
        new Vector3(10, 5, -2),
    };
    int currentTarget = 0;

    public void move(Vector3 currentRoomMin, Vector3 currentRoomMax) {
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

                if (MOVEMENT_MODE == MovementMode.FIXED_BOX) {
                    final float X_Z_METERS = 4.0f;
                    final float Y_TO_TOP_METERS = 1.5f;
                    final float Y_TO_BOTTOM_METERS = 1.0f;
                    randomTargetPosition.set(
                            ((float) Math.random()) * X_Z_METERS * 2 - X_Z_METERS,
                            ((float) Math.random()) * X_Z_METERS * 2 - X_Z_METERS,
                            ((float) Math.random()) * (Y_TO_BOTTOM_METERS + Y_TO_TOP_METERS) - Y_TO_BOTTOM_METERS);
                } else {
                    randomTargetPosition.set(
                            lerp(roomMin.x, roomMax.x, (float) Math.random()),
                            lerp(roomMin.y, roomMax.y, (float) Math.random()),
                            lerp(roomMin.z, roomMax.z, (float) Math.random())
                    );
                }
            }
            randomCurrentPosition.set(randomLastPosition);
            randomCurrentPosition.lerp(randomTargetPosition, easeInOutQuad(interpTime));
            instance.transform.setTranslation(randomCurrentPosition);
            return;
        }
        // Phase.CAPTURING:

        position.add(velocity.scl(Gdx.graphics.getDeltaTime()));

        // instance.transform.setToTranslation(position);
        // instance.transform.setToLookAt(position, target, new Vector3(0, 1, 0));
        // instance.transform.scale(0.5f, 0.5f, 0.5f);

        // Quaternion currentRotation = new Quaternion();
        // instance.transform.getRotation(currentRotation, true);
        // new Matrix4().setToLookAt(position, target, new Vector3(0, 1, 0)).to;
        instance.transform.avg(new Matrix4().setToLookAt(position, target, new Vector3(0, 1, 0)), 1 - 2f * Gdx.graphics.getDeltaTime());
        instance.transform.setTranslation(position);

        // Vector3 acceleration = new Vector3(target).sub(position).nor().scl(SPEED * Gdx.graphics.getDeltaTime());
        float[] values = instance.transform.getValues();
        Vector3 acceleration = new Vector3(values[Matrix4.M00], values[Matrix4.M01], values[Matrix4.M02]).nor().scl(SPEED * Gdx.graphics.getDeltaTime());
        if (position.dst(target) > 0.2f) {
            velocity.add(acceleration);
        } else {
            velocity.scl(0.9999f);
        }

        Vector3 t = new Vector3();
        instance.transform.getTranslation(t);
        t.lerp(target, Gdx.graphics.getDeltaTime() * 10);
        instance.transform.setToTranslation(t);

        // instance.transform.setToWorld(position, acceleration.nor(), new Vector3(0, 1, 0));
        // instance.transform.scale(0.3f, 0.3f, 0.3f);
    }

    public void setTarget(Vector3 t) {
        target.set(t);
    }

    public Vector3 getTarget() {
        return target;
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        particles.setPositionFrom(instance.transform);
        particles.draw(modelBatch);
        modelBatch.render(instance, environment);

        for (Shot shot : shots) {
            shot.draw(modelBatch, environment);
        }
    }

    public void shoot(Ray ray) {
        shots.add(new Shot(ray.cpy()));

        if (hitByRay(ray)) {
            Log.e("demon-go-capturing", "hit!");
            enterCapturingPhase();
        } else Log.e("demon-go-capturing", "miss...");
    }

    private boolean hitByRay(Ray ray) {
        Vector3 intersection = new Vector3();
        return Intersector.intersectRaySphere(ray, randomCurrentPosition, SPHERE_SIZE / 2, intersection);
    }

    private void enterCapturingPhase() {
        if (phase == Phase.CAPTURING) {
            return;
        }

        Log.e("demon-go-capturing", "GOT YA");
        phase = Phase.CAPTURING;
        ModelInstance newInstance = new ModelInstance(demonModel);
        position = randomCurrentPosition;
        instance = newInstance;
    }
}
