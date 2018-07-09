package com.github.demongo;

import android.content.Context;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.exceptions.NotYetAvailableException;

import org.opencv.android.OpenCVLoader;

import java.util.Collection;

import hpi.gitlab.demongo.pipeline.NullStep;
import hpi.gitlab.demongo.pipeline.Pipeline;

public class DemonGoGame extends ARCoreScene {
	private AssetManager assetManager;
	private Environment environment;

	private boolean loading = true;
	private Overlay overlay;
	private Hud hud;
	private PvP pvp = null;
	private ARDebug arDebug;

	private ARDemon demon;

	private Pipeline pipeline;
	private AngleChangeStep angleChangeStep;


	private ARSnapshot lastSnapshot = null;

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

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		arDebug = new ARDebug();
		overlay = new Overlay();
		hud = new Hud(context, context.getResources().getDisplayMetrics().density, new Hud.TriggerListener() {
			@Override
			public void onPvPStarted() {
				pvp = new PvP(context, hud);
			}
		});

		Config config = new Config(getSession());
		config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
		getSession().configure(config);
	}

	private void assetsLoaded() {
	    demon = new ARDemon(getCamera(), assetManager);
		loading = false;
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

	private void input(Frame frame) {
	    // temporary solution for guiding the demon, to be replaced by pipeline's movement direction
	    if (Gdx.input.justTouched() && lastSnapshot != null) {
            demon.setTarget(lastSnapshot.projectPoint(Gdx.input.getX(), Gdx.input.getY()));
        }
    }

	@Override
	public void render(Frame frame, ModelBatch modelBatch) {
		if (loading && assetManager.update()) {
			assetsLoaded();
		}

		if (!getSession().getAllTrackables(Plane.class).isEmpty()) {
		    hud.setLoading(false);
		}

        if (angleChangeStep.checkPictureTransformDelta(getCamera().view.cpy())) {
        	overlay.signalNewAngle();
		}
		input(frame);

		if (demon != null) {
			demon.move();
        }

		try {
			lastSnapshot = new ARSnapshot(1.0, frame);
			pipeline.add(lastSnapshot);
		} catch (NotYetAvailableException e) {
			lastSnapshot = null;
			Log.e("demon-go", "no image yet");
		}

		demon.render(modelBatch, environment);

		arDebug.update(frame);
		arDebug.draw(modelBatch, environment);
	}

	@Override
	public void resize(int width, int height) {
	    super.resize(width, height);
		overlay.resize();
	}

	@Override
	protected void postRender(Frame frame) {
	    overlay.render(frame.getCamera().getPose());

	    hud.draw();
	}
}
