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
import com.badlogic.gdx.utils.Timer;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.github.demongo.Map.MapActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.android.OpenCVLoader;

import hpi.gitlab.demongo.pipeline.NullStep;
import hpi.gitlab.demongo.pipeline.Pipeline;

public class DemonGoGame extends ARCoreScene {
	private static final int SECONDS_SPELL_SEARCH_TIMEOUT = 10;
	private static final int MIN_FRAME_WAIT = 20;

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

	private ARSnapshot lastSnapshot;
	private boolean paused = true;

	private Timer.Task catchSpellTimeout;

	private int frameCounter = 0;

	private ConfidentVector3[] bestVectors = {
        new ConfidentVector3(new Vector3(-30, 1, 0), 0),
        new ConfidentVector3(new Vector3(30, 1, 0), 0),
        new ConfidentVector3(new Vector3(0, 1, -30), 0),
        new ConfidentVector3(new Vector3(0, 1, 30), 0)
	};

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
			    if (phase == ARDemon.Phase.CAPTURING) {

			        scheduleRescueSpellTimer();

					Float[] points = pipeline.requestTargets();
                    Vector3[] targets = new Vector3[points.length / 3];
                    for (int i = 0; i < targets.length; i++) {
                        targets[i] = new Vector3(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]);
                    }
                    demon.setTargets(targets, getSession(), bestVectors);

					for (Anchor anchor : demon.getAnchors())
                        arDebug.addTargetPoint(new Vector3(anchor.getPose().getTranslation()));
				}
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
			    spellCompleted();
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
		// session.setCameraConfig(session.getSupportedCameraConfigs().get(2));
		session.configure(config);
		try {
            session.resume();
		} catch (CameraNotAvailableException e) {
			Log.e("demon-go-camera", "camera not available");
		}

		paused = false;
	}

	private void spellCompleted() {
		waitingForSpellCompletion = false;

		if (lastSnapshot != null) {
			pipeline.sendImmediately(lastSnapshot.copyWithNewScore(9999999.0));
			Log.d("demon-go-spell", "Sending full frame.");
		}

		if (!demon.moveToNextTarget()) {
			Log.e("demon-go", "A winner is you!");
			demon.setCaptured();
			Intent intent = new Intent(context, MapActivity.class);
			intent.putExtra("demon-captured", true);
			context.startActivity(intent);
			return;
		}
		scheduleRescueSpellTimer();
	}

	private void scheduleRescueSpellTimer() {
		if (catchSpellTimeout != null) {
			catchSpellTimeout.cancel();
		}
		catchSpellTimeout = new Timer.Task() {
			@Override
			public void run() {
				catchSpellTimeout = null;
				hud.showSpell();
			}
		};
		new Timer().scheduleTask(catchSpellTimeout, SECONDS_SPELL_SEARCH_TIMEOUT);
	}

	private void maybeUseBestVector(ConfidentVector3 vector) {
		int closest = 0;
		float distance = vector.distance(bestVectors[0]);
		for (int i = 1; i < bestVectors.length; i++) {
			float newDistance = vector.distance(bestVectors[i]);
			if (newDistance < distance) {
				distance = newDistance;
				closest = i;
			}
		}
		if (vector.confidence > bestVectors[closest].confidence)
            bestVectors[closest] = vector;
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

		boolean angleChanged = angleChangeStep.checkPictureTransformDelta(getCamera().view.cpy());

		frameCounter += 1;
		if (angleChanged && frameCounter >= MIN_FRAME_WAIT) {
            lastSnapshot = null;
            try {
               Thread.sleep(100);
           } catch(InterruptedException ex)
           {
               Thread.currentThread().interrupt();
           }
			try {
				lastSnapshot = new ARSnapshot(1.0, frame);
				maybeUseBestVector(lastSnapshot.bestTracked);
				pipeline.add(lastSnapshot);
			} catch (NotYetAvailableException e) {
				Log.e("demon-go", "no image yet");
			}
			frameCounter = 0;
		}

		Vector3 cameraPosition = new Vector3(frame.getCamera().getPose().getTranslation());
		demon.move(lastSnapshot != null ? lastSnapshot.min : null, lastSnapshot != null ? lastSnapshot.max : null, cameraPosition);
		// getCamera().view.getTranslation(cameraPosition);

		float distanceToDemon = demon.getPosition().dst(cameraPosition);

		if (!waitingForSpellCompletion && demon.getPhase() == ARDemon.Phase.CAPTURING && demon.getCurrentTarget().dst(cameraPosition) < 0.5) {
			hud.showSpell();
			if (catchSpellTimeout != null) {
                catchSpellTimeout.cancel();
				catchSpellTimeout = null;
			}
			waitingForSpellCompletion = true;
		}

		arDebug.update(frame, demon, distanceToDemon, "Camera: " + cameraPosition.toString() + "\n" + "Demon: " + demon.getPosition().toString() + "\n");
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
