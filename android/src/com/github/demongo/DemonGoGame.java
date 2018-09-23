package com.github.demongo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.github.demongo.Map.MapActivity;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.android.OpenCVLoader;

import hpi.gitlab.demongo.pipeline.Pipeline;

public class DemonGoGame extends ARCoreScene {
	private AssetManager assetManager;
	private Environment environment;

	private Overlay overlay;
	private Hud hud;
	private PvP pvp = null;
	private ARDebug arDebug;
	private boolean waitingForSpellCompletion = false;

	private ARDemon demon;

	private Pipeline pipeline;
	private AngleChangeStep angleChangeStep;

	private Context context;

	private boolean paused = true;

	DemonGoGame(Context context) {
		this.context = context;
	}

	@Override
	public void create () {
		super.create();

		OpenCVLoader.initDebug();

		angleChangeStep = new AngleChangeStep();
		// currently angle change is disabled for debugging
		pipeline = new Pipeline(context, angleChangeStep);

		assetManager = new AssetManager();
		arDebug = new ARDebug();
		demon = new ARDemon(getCamera(), assetManager, new ARDemon.PhaseChangedListener() {
			@Override
			public void changed(ARDemon demon, ARDemon.Phase phase) {
                Float[] points = pipeline.requestTargets();
                Vector3[] targets = new Vector3[points.length / 3];
			    for (int i = 0; i < targets.length; i++) {
			    	targets[i] = new Vector3(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]);
			    	arDebug.addTargetPoint(targets[i]);
				}
				demon.setTargets(targets, getSession());
			}
		});

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		overlay = new Overlay();
		hud = new Hud(context, context.getResources().getDisplayMetrics().density, new Hud.TriggerListener() {
			@Override
			public void onPvPStarted() {
			    // can currently not be reached because the button is disabled
				pvp = new PvP(context);
			}

			@Override
			public void onSpellCompleted() {
				if (!demon.moveToNextTarget()) {
					Log.e("demon-go", "A winner is you!");
					demon.setCaptured();

					Intent intent = new Intent(context, MapActivity.class);
					intent.putExtra("demon-captured", true);
					context.startActivity(intent);
				}
				waitingForSpellCompletion = false;
			}
		}, arDebug);

		final Session session = getSession();

		for (CameraConfig c : session.getSupportedCameraConfigs()) {
			Log.e("demon-go-camera", "Config: " + c.getImageSize().toString() +  " " + c.getTextureSize().toString());
		}

		session.pause();
		Config config = new Config(session);
		config.setFocusMode(Config.FocusMode.AUTO);
		// config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
		session.setCameraConfig(session.getSupportedCameraConfigs().get(2));
		session.configure(config);
		try {
            session.resume();
		} catch (CameraNotAvailableException e) {
			Log.e("demon-go-camera", "camera not available");
		}

		paused = false;
	}

    private void update(Frame frame) {
	    if (paused) {
	    	return;
		}

		if (!getSession().getAllTrackables(Plane.class).isEmpty()) {
			hud.setLoading(false);
		}

		if (Gdx.input.justTouched()) {
			Ray pickRay = getCamera().getPickRay(Gdx.input.getX(), Gdx.input.getY());
			demon.shoot(pickRay);
		}

		angleChangeStep.checkPictureTransformDelta(getCamera().view.cpy());

		ARSnapshot lastSnapshot = null;
		try {
			lastSnapshot = new ARSnapshot(1.0, frame);
			pipeline.add(lastSnapshot);
		} catch (NotYetAvailableException e) {
//			Log.e("demon-go", "no image yet");
		}

		demon.move(lastSnapshot != null ? lastSnapshot.min : null, lastSnapshot != null ? lastSnapshot.max : null, getCamera().position);
		Vector3 cameraPosition = new Vector3(frame.getCamera().getPose().getTranslation());
		getCamera().view.getTranslation(cameraPosition);

		float distanceToDemon = demon.getCurrentTarget().dst(cameraPosition);

		if (!waitingForSpellCompletion && demon.getPhase() == ARDemon.Phase.CAPTURING && distanceToDemon < 2) {
			hud.showSpell();
			waitingForSpellCompletion = true;
		}

		arDebug.update(frame, demon, distanceToDemon);
	}

	@Override
	public void render(Frame frame, ModelBatch modelBatch) {
		update(frame);

		modelBatch.begin(getCamera());
        demon.render(modelBatch, environment);
		arDebug.draw(modelBatch, environment);
		modelBatch.end();
	}

	@Override
	public void resize(int width, int height) {
	    super.resize(width, height);
		overlay.resize();
	}

	@Override
	protected void postRender(Frame frame) {
	    // overlay.render(frame.getCamera().getPose());
	    hud.draw();
	}

	@Override
	public void pause() {
		super.pause();
		paused = true;
	}

	@Override
	public void resume() {
		super.resume();
		paused = false;
	}
}
