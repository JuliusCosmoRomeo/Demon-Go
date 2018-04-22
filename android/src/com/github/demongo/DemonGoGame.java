package com.github.demongo;

import android.media.Image;
import android.util.Log;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.Array;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.google.ar.core.Frame;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;

public class DemonGoGame extends ARCoreScene {
	private Array<ModelInstance> instances = new Array<ModelInstance>();
	private AssetManager assetManager;
	private Environment environment;
	private ParticleSystem particleSystem;

	private boolean loading = true;

	@Override
	public void create () {
		super.create();

		OpenCVLoader.initDebug();

        particleSystem = new ParticleSystem();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		assetManager = new AssetManager();
		assetManager.load("demon01.g3db", Model.class);
	}

	private void assetsLoaded() {
        instances.add(new ModelInstance(assetManager.get("demon01.g3db", Model.class)));
	    loading = false;
    }

    private ParticleEffect createParticleSystem() {
        PointSpriteParticleBatch batch = new PointSpriteParticleBatch();
        batch.setCamera(getCamera());
        particleSystem.add(batch);

        assetManager.load("test.pfx",
                ParticleEffect.class,
                new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches()));
        assetManager.finishLoading();

        ParticleEffect effect = assetManager.get("test.pfx");
        effect.init();
        effect.start();
        particleSystem.add(effect);
        return effect;
    }

	@Override
	public void render (Frame frame, ModelBatch modelBatch) {
	    if (loading && assetManager.update()) {
	        assetsLoaded();
        }

        try {
            Image image = frame.acquireCameraImage();
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
            byte[] pixels = new byte[buffer.limit()];
            buffer.position(0);
            buffer.get(pixels);
            mat.put(0, 0, pixels);
            image.close();
		} catch (NotYetAvailableException e) {
			Log.e("demon-go", "no image yet");
		}

        particleSystem.updateAndDraw();

		modelBatch.render(instances, environment);
        modelBatch.render(particleSystem, environment);
	}

	@Override
	public void dispose () {
		instances.clear();
	}
}
