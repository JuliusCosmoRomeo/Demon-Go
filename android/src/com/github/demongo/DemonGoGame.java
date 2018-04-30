package com.github.demongo;

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

public class DemonGoGame extends ARCoreScene {
	private Array<ModelInstance> instances = new Array<>();
	private AssetManager assetManager;
	private Environment environment;
	private ParticleSystem particleSystem;
	private Model pointCubeModel;

	private boolean loading = true;
	private Overlay overlay;

	private Array<ModelInstance> pointCubes = new Array<>();
	private ModelInstance demon;
	Vector3 demonTarget = new Vector3(0, 0, 0);
	Anchor demonAnchor = null;
	Matrix4 lastPictureTransform = null;

	private final float POINT_SIZE = 0.03f;

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

		overlay = new Overlay();

		createPointCube();
	}

	private void createPointCube() {
		ModelBuilder modelBuilder = new ModelBuilder();
		pointCubeModel = modelBuilder.createBox(POINT_SIZE, POINT_SIZE, POINT_SIZE,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
	}

	private void assetsLoaded() {
	    demon = new ModelInstance(assetManager.get("demon01.g3db", Model.class));
		instances.add(demon);
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

	private void input(Frame frame) {
	    if (!Gdx.input.justTouched()) {
	        return;
        }

        float x = Gdx.input.getX();
        float y = Gdx.input.getY();

        for (HitResult hit : frame.hitTest(x, y)) {
            if (demonAnchor != null)
                demonAnchor.detach();

            demonAnchor = hit.createAnchor();
            // we only use the closest hit, if any
            break;
        }
    }

    private void checkPictureTransformDelta() {
	    Vector3 lastPosition = new Vector3();
	    Vector3 currentPosition = new Vector3();

	    Matrix4 current = getCamera().view;

	    current.getTranslation(currentPosition);
        lastPictureTransform.getTranslation(lastPosition);

        Quaternion lastRotation = new Quaternion();
        Quaternion currentRotation = new Quaternion();

        current.getRotation(currentRotation);
        lastPictureTransform.getRotation(lastRotation);

        float lastAngle = lastRotation.getAngle();
        float currentAngle = currentRotation.getAngle();

        if (lastPosition.dst(currentPosition) > 0.2 || Math.abs(lastAngle - currentAngle) > 10) {
            lastPictureTransform.set(current);
            overlay.signalNewAngle();
        }
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

        if (lastPictureTransform == null) {
            lastPictureTransform = getCamera().view.cpy();
        }
		checkPictureTransformDelta();
		input(frame);

        if (demonAnchor != null) {
            demonTarget.set(demonAnchor.getPose().tx(), demonAnchor.getPose().ty(), demonAnchor.getPose().tz());
        }

		if (demon != null) {
            Vector3 current = new Vector3();
            demon.transform.getTranslation(current);
            demon.transform.setToTranslation(current.lerp(demonTarget, 2 * Gdx.graphics.getDeltaTime()));
            demon.transform.scale(0.5f, 0.5f, 0.5f);
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
		modelBatch.render(pointCubes, environment);
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

	@Override
	public void dispose () {
		instances.clear();
	}
}
