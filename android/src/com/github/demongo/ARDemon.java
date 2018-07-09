package com.github.demongo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class ARDemon {

    private Vector3 target = new Vector3();

    private static final float SPEED = 30;
    private static final String MODEL_PATH = "demon01.g3db";

    private ModelInstance instance;
    private ParticleSystem particleSystem;
    private Vector3 velocity = new Vector3();
    private Vector3 position = new Vector3();

    ARDemon(Camera camera, AssetManager assetManager) {
        assetManager.load(MODEL_PATH, Model.class);
        assetManager.finishLoading();
        instance = new ModelInstance(assetManager.get(MODEL_PATH, Model.class));

        particleSystem = createParticleSystem(assetManager, camera);
    }

    public void move() {
        instance.transform.getTranslation(position);
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
        particleSystem.updateAndDraw();

        modelBatch.render(instance, environment);
        modelBatch.render(particleSystem, environment);
    }

    private ParticleSystem createParticleSystem(AssetManager assetManager, Camera camera) {
        ParticleSystem particleSystem = new ParticleSystem();

        PointSpriteParticleBatch batch = new PointSpriteParticleBatch();
        batch.setCamera(camera);
        particleSystem.add(batch);

        assetManager.load("test.pfx",
                ParticleEffect.class,
                new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches()));
        assetManager.finishLoading();

        /*ParticleEffect effect = ((ParticleEffect) assetManager.get("test.pfx")).copy();
        effect.init();
        effect.start();
        particleSystem.add(effect);*/

        return particleSystem;
    }

    private ModelInstance createModelInstane(AssetManager assetManager) {
        assetManager.load("demon01.g3db", Model.class);
        assetManager.finishLoading();
        return new ModelInstance(assetManager.get("demon01.g3db", Model.class));
    }
}
