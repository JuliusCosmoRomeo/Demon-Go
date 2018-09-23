package com.github.demongo;

import android.content.Context;
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
import com.google.ar.core.Anchor;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.android.OpenCVLoader;

import java.util.Collection;

import hpi.gitlab.demongo.pipeline.NullStep;
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

	DemonGoGame(Context context) {
		this.context = context;
	}

	@Override
	public void create () {
		super.create();

		OpenCVLoader.initDebug();

		angleChangeStep = new AngleChangeStep();
		// currently angle change is disabled for debugging
		pipeline = new Pipeline(context, new NullStep());

		assetManager = new AssetManager();
		demon = new ARDemon(getCamera(), assetManager, new ARDemon.PhaseChangedListener() {
			@Override
			public void changed(ARDemon demon, ARDemon.Phase phase) {
			    float[] points = pipeline.requestTargets();
			    Vector3[] targets = new Vector3[points.length / 3];
			    for (int i = 0; i < targets.length; i++) {
			    	targets[i] = new Vector3(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]);
				}
				demon.setTargets(targets, getSession());
			}
		});

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		arDebug = new ARDebug();
		overlay = new Overlay();
		hud = new Hud(context, context.getResources().getDisplayMetrics().density, new Hud.TriggerListener() {
			@Override
			public void onPvPStarted() {
				pvp = new PvP(context);
			}

			@Override
			public void onSpellCompleted() {
				if (!demon.moveToNextTarget()) {
					// TODO move to "you caught the demon" screen
					Log.e("demon-go", "A winner is you!");
					demon.setCaptured();
				}
				waitingForSpellCompletion = false;
			}
		});

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
	}

	private Anchor cloudAnchor;
	private void maybeCreatePvPCloudAnchor() {
	    Collection<Plane> planes = getSession().getAllTrackables(Plane.class);
		if (pvp != null && !planes.isEmpty()) {
            cloudAnchor = getSession().createAnchor(planes.iterator().next().getCenterPose());
            getSession().hostCloudAnchor(cloudAnchor);
        }
	}

	private void checkCloudAnchor() {
	    if (cloudAnchor == null)
	        return;

	    Anchor.CloudAnchorState state = cloudAnchor.getCloudAnchorState();
	    if (state.isError()) {
            // TODO
	        Log.e("demon-go-pvp", "failed to create cloud anchor!");
        } else if (state == Anchor.CloudAnchorState.SUCCESS) {
	        pvp.updateCloudAnchorId(cloudAnchor.getCloudAnchorId());
        }
    }

    public void update(Frame frame) {
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
			// pipeline.add(lastSnapshot);
		} catch (NotYetAvailableException e) {
			Log.e("demon-go", "no image yet");
		}

		demon.move(lastSnapshot != null ? lastSnapshot.min : null, lastSnapshot != null ? lastSnapshot.max : null, getCamera().position);
		Vector3 cameraPosition = new Vector3(frame.getCamera().getPose().getTranslation());
		getCamera().view.getTranslation(cameraPosition);
		if (!waitingForSpellCompletion && demon.getPhase() == ARDemon.Phase.CAPTURING)
			Log.e("demon-go-capturing", Float.toString(demon.getCurrentTarget().dst(cameraPosition)) + " " + demon.getCurrentTarget().toString() + " " + cameraPosition.toString());

		if (!waitingForSpellCompletion && demon.getPhase() == ARDemon.Phase.CAPTURING && demon.getCurrentTarget().dst(cameraPosition) < 20) {
			hud.showSpell();
			waitingForSpellCompletion = true;
		}

		arDebug.update(frame);
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
}
