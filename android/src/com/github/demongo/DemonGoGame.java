package com.github.demongo;

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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.utils.Array;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.google.ar.core.Frame;

public class DemonGoGame extends ARCoreScene {
	private PerspectiveCamera camera;
	private Array<ModelInstance> instances = new Array<ModelInstance>();
	private Environment environment;
	private AssetManager assetManager;

	private boolean loading = true;

	@Override
	public void create () {
		super.create();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		camera = new PerspectiveCamera(80, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(1f, 1f, 1f);
		camera.lookAt(0, 0, 0);
		camera.near = 1f;
		camera.far = 300f;
		camera.update();

		Gdx.input.setInputProcessor(new CameraInputController(camera));

		assetManager = new AssetManager();
		assetManager.load("demon01.g3db", Model.class);
	}

	private void assetsLoaded() {
        instances.add(new ModelInstance(assetManager.get("demon01.g3db", Model.class)));
	    loading = false;
    }

	@Override
	public void render (Frame frame, ModelBatch modelBatch) {
	    if (loading && assetManager.update()) {
	        assetsLoaded();
        }
		modelBatch.render(instances, environment);
	}
	
	@Override
	public void dispose () {
		instances.clear();
	}
}
