package com.github.demongo;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;

import java.nio.FloatBuffer;

public class ARDebug {
    private Array<ModelInstance> pointCubes = new Array<>();
    private Model pointCubeModel;
    private ModelInstance originIndicator;

    private final float POINT_SIZE = 0.03f;

    ARDebug() {
        createPointCube();
        createOriginIndicator();
    }

    public void update(Frame frame) {
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
    }

    public void draw(ModelBatch modelBatch, Environment environment) {
        modelBatch.render(originIndicator, environment);
        // modelBatch.render(pointCubes, environment);
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
}
