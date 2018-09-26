package com.github.demongo;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;

import java.nio.FloatBuffer;

public class ARDebug {
    private Array<ModelInstance> pointCubes = new Array<>();
    private Array<ModelInstance> targetPoints = new Array<>();
    private Model pointCubeModel;
    private Model sphereModel;
    private ModelInstance originIndicator;
    private ModelInstance roomSizeIndicator;

    private final float POINT_SIZE = 0.03f;
    private final float TRACKING_SPHERE_SIZE = 0.1f;

    private String infoString;
    private final boolean IS_DEBUG_MODE = false;

    ARDebug() {
        createPointCube();
        createOriginIndicator();
        createTargetPointIndicator();
        createRoomSizeIndicator();
    }

    public void update(Frame frame, ARDemon demon, float distanceToDemon, String other) {
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

            cube.materials.get(0).set(ColorAttribute.createDiffuse(Color.RED.cpy().lerp(Color.GREEN, confidence)));
            num++;
        }

        cloud.release();

        updateRoomSize(demon.getRoomMin(), demon.getRoomMax());

        infoString = "Debug:\n" +
                other +
                "Phase: " + demon.getPhase().toString() + "\n" +
                (demon.getPhase() == ARDemon.Phase.CAPTURING ? "Distance to Demon: " + distanceToDemon + "\n" : "");
    }

    public void draw(ModelBatch modelBatch, Environment environment) {
        if(IS_DEBUG_MODE) {
            modelBatch.render(originIndicator, environment);
            modelBatch.render(pointCubes, environment);
            modelBatch.render(targetPoints, environment);
            modelBatch.render(roomSizeIndicator);
        }
    }

    private Vector3 tmp1 = new Vector3();
    private Vector3 tmp2 = new Vector3();
    private void updateRoomSize(Vector3 roomMin, Vector3 roomMax) {
        roomSizeIndicator.transform
            .idt()
            .translate(tmp1.set(roomMin).add(tmp2.set(roomMax).sub(roomMin).scl(0.5f)))
            .scale(roomMax.x - roomMin.x, roomMax.y - roomMin.y, roomMax.z - roomMin.z);
    }

    private void createRoomSizeIndicator() {
        ModelBuilder modelBuilder = new ModelBuilder();
        final Model cubeModel = modelBuilder.createBox(1, 1, 1,
                GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        roomSizeIndicator = new ModelInstance(cubeModel);
    }

    private void createTargetPointIndicator() {
        sphereModel = new ModelBuilder().createSphere(TRACKING_SPHERE_SIZE, TRACKING_SPHERE_SIZE, TRACKING_SPHERE_SIZE, 10,10,
                new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
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

    public void addTargetPoint(Vector3 point) {
        ModelInstance instance = new ModelInstance(sphereModel);
        instance.transform.setToTranslation(point);
        targetPoints.add(instance);
    }

    public String getInfoString() {
        if(IS_DEBUG_MODE) {
            return infoString;
        } else {
            return "";
        }
    }
}
