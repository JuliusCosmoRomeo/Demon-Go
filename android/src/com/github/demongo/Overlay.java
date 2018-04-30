package com.github.demongo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.google.ar.core.Pose;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;

public class Overlay {

    private ModelBatch modelBatch;
    public ModelInstance arrow;
    private Camera camera;

    public Model arrowModel;

    public Overlay() {
        modelBatch = new ModelBatch();

        arrowModel = new ModelBuilder().createArrow(0, 0, 0, 0, 0.1f, 0, 0.2f, 0.6f, 12, GL_TRIANGLES,
                new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        arrow = new ModelInstance(arrowModel);

        camera = new OrthographicCamera(1, 1);
        resize();
    }

    public void signalNewAngle() {
        Color color = new Color();
        color.fromHsv((float) Math.random() * 360, 1, 1);
        arrow.materials.get(0).set(ColorAttribute.createDiffuse(color));
    }

    public void render(Pose pose) {
        float vm[] = new float[16];
        pose.toMatrix(vm, 0);
        arrow.transform.set(vm);
        arrow.transform.setTranslation(0, 0, -10);

        modelBatch.begin(camera);
        modelBatch.render(arrow);
        modelBatch.end();
    }

    public void resize() {
        camera.viewportWidth = Gdx.graphics.getWidth() * 0.001f;
        camera.viewportWidth = Gdx.graphics.getHeight() * 0.001f;
        camera.update();
    }
}
