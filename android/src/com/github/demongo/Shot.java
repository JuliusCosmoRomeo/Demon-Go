package com.github.demongo;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class Shot {
    private static final float DISTANCE_NEAR_PLANE = 0.2f;
    private static final float FADE_OUT_SPEED_FACTOR = 0.9f;

    private ModelInstance instance;

    private float alpha = 1.0f;

    Shot(Ray ray) {
        ModelBuilder modelBuilder = new ModelBuilder();

        Vector3 dest = ray.origin.cpy().add(ray.direction.cpy().scl(10.0f));
        ray.origin.add(ray.direction.scl(DISTANCE_NEAR_PLANE));
        Model model = modelBuilder.createArrow(ray.origin.x, ray.origin.y, ray.origin.z,
                dest.x, dest.y, dest.z,
                0.01f,
                0.1f,
                5,
                GL20.GL_TRIANGLES,
                new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        instance = new ModelInstance(model);
    }

    public void draw(ModelBatch batch, Environment environment) {
        alpha -= Gdx.graphics.getDeltaTime() * FADE_OUT_SPEED_FACTOR;

        instance.materials.get(0).set(new BlendingAttribute(alpha));
        batch.render(instance, environment);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
