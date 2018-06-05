package com.github.demongo;

import android.content.Context;
import android.media.Image;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.PointCloud;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;

import hpi.gitlab.demongo.pipeline.Pipeline;

public class DemonGoGame extends ARCoreScene {
	private AssetManager assetManager;
	private Environment environment;
	private Model pointCubeModel;
	private ModelInstance originIndicator;

	private boolean loading = true;
	private Overlay overlay;

	private Array<ModelInstance> pointCubes = new Array<>();
	private Demon demon;
	private Anchor demonAnchor = null;

	private Pipeline pipeline;
	private AngleChangeStep angleChangeStep;

	private final float POINT_SIZE = 0.03f;

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
		pipeline = new Pipeline(context, angleChangeStep);

		assetManager = new AssetManager();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		overlay = new Overlay();

		createPointCube();
		createOriginIndicator();
	}

	private void createOriginIndicator() {
		ModelBuilder modelBuilder = new ModelBuilder();
		originIndicator = new ModelInstance(modelBuilder.createXYZCoordinates(1, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorPacked));
	}

	private void createPointCube() {
		ModelBuilder modelBuilder = new ModelBuilder();
		pointCubeModel = modelBuilder.createBox(POINT_SIZE, POINT_SIZE, POINT_SIZE,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
	}

	private void assetsLoaded() {
	    demon = new Demon(getCamera(), assetManager);
		loading = false;
	}

	private void input(Frame frame) {
	    if (!Gdx.input.justTouched()) {
	        return;
        }

        float x = Gdx.input.getX();
        float y = Gdx.input.getY();

        if (lastSnapshot != null) {
        	demon.setTarget(lastSnapshot.projectPoint(x, y));
        	Log.e("demon-go-ar", demon.getTarget().toString());
		}

        /*for (HitResult hit : frame.hitTest(x, y)) {
            if (demonAnchor != null)
                demonAnchor.detach();

            demonAnchor = hit.createAnchor();
            // we only use the closest hit, if any
            break;
        }*/
    }

	@Override
	public void render (Frame frame, ModelBatch modelBatch) {
		if (loading && assetManager.update()) {
			assetsLoaded();
		}

		PointCloud cloud = frame.acquirePointCloud();
		FloatBuffer points = cloud.getPoints();

		int num = 0;
		while (points.hasRemaining()) {
		    ModelInstance cube;
			if (num < pointCubes.size) {
			    cube = pointCubes.get(num);
			} else {
                cube = new ModelInstance(pointCubeModel);
                pointCubes.add(cube);
			}

			cube.transform.setToTranslation(points.get(), points.get(), points.get());
			float confidence = points.get();

            cube.materials.get(0).set(ColorAttribute.createDiffuse(Color.RED.lerp(Color.GREEN, confidence)));
			num++;
		}

		cloud.release();

		/*Collection<Plane> planes = getSession().getAllTrackables(Plane.class);
		if (!planes.isEmpty()) {
			Pose pose = planes.iterator().next().getCenterPose();
			demonTarget.set(pose.tx(), pose.ty(), pose.tz());
		}*/

        if (angleChangeStep.checkPictureTransformDelta(getCamera().view.cpy())) {
        	overlay.signalNewAngle();
		}
		input(frame);

		if (demon != null) {
			// if (demonAnchor != null) {
				// demonTarget.set(demonAnchor.getPose().tx(), demonAnchor.getPose().ty(), demonAnchor.getPose().tz());
			// }
			if (false && num > 0) {
				// pointCubes.get(num - 1).transform.getTranslation(demon.target);
			}
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
		modelBatch.render(pointCubes, environment);
		modelBatch.render(originIndicator, environment);
	}

	@Override
	public void resize(int width, int height) {
	    super.resize(width, height);
		overlay.resize();
	}

	@Override
	protected void postRender(Frame frame) {
	    overlay.render(frame.getCamera().getPose());
	}
}
