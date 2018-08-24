package com.github.demongo;

import android.opengl.Matrix;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class DemonParticles {

    private static final String RESOURCE = "flames.pfx";

    private ParticleSystem system;
    private ParticleEffect effect;
    private Matrix4 transform = new Matrix4().idt();

    DemonParticles(Camera camera) {
        system = new ParticleSystem();

        BillboardParticleBatch batch = new BillboardParticleBatch();
        batch.setCamera(camera);
        system.add(batch);

        AssetManager assets = new AssetManager();
        ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(system.getBatches());
        assets.load(RESOURCE, ParticleEffect.class, loadParam);
        assets.finishLoading();

        ParticleEffect originalEffect = assets.get(RESOURCE);
        effect = originalEffect.copy();
        effect.init();
        effect.start();
        system.add(effect);
    }

    void setPositionFrom(Matrix4 ref) {
        transform.set(ref);
    }

    void draw(ModelBatch modelBatch) {
        effect.setTransform(transform);

        system.update();
        system.begin();
        system.draw();
        system.end();
        modelBatch.render(system);
    }
}
