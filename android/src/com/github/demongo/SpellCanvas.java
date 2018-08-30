package com.github.demongo;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector2;

public class SpellCanvas {
    interface CompletionListener {
        void completed();
    }

    private static final float WIDTH = 40;

    private float canvasWidth;
    private float canvasHeight;

    private ShapeRenderer renderer = new ShapeRenderer();
    private CompletionListener completionListener;

    private float mistakeFlashDuration = 0;

    SpellCanvas(float width, float height, CompletionListener listener) {
        completionListener = listener;

        canvasWidth = width;
        canvasHeight = height;

        newSpell();
    }

    private Vector2[] points;

    public void newSpell() {
        int steps = 3;
        points = new Vector2[steps];

        for (int i = 0; i < steps; i++) {
            points[i] = new Vector2((float) Math.random() * canvasWidth, (float) Math.random() * canvasHeight);
        }
    }

    private float closestPointOnSegment(Vector2 p, Vector2 v, Vector2 w, Vector2 closest) {
        Vector2 vw = w.cpy().sub(v);
        Vector2 vp = p.cpy().sub(v);
        float t = vw.dot(vp) / vw.len2();
        closest.set(v).add(vw.scl(t));
        return t;
    }

    private static final float MAX_DISTANCE = 120;
    private int progress = 0;
    private float subProgress = 0;

    void resetProgress() {
        progress = 0;
        subProgress = 0;
    }

    private void flashMistake() {
        mistakeFlashDuration = 0.3f;
    }

    void checkProgress() {
        if (progress >= points.length - 1)
            return;

        Vector2 mouse = new Vector2(Gdx.input.getX(), canvasHeight - Gdx.input.getY());
        Vector2 onLine = new Vector2();
        float t = closestPointOnSegment(mouse, points[progress], points[progress + 1], onLine);

        if (mouse.dst(onLine) > MAX_DISTANCE) {
            resetProgress();
            flashMistake();
            return;
        }

        subProgress = Math.min(Math.max(t, 0), 1);
        if (mouse.dst(points[progress + 1]) < MAX_DISTANCE / 2) {
            progress++;
            Log.e("demon-go-spell", "progress " + Integer.toString(progress) + " / " + Integer.toString(points.length));
            subProgress = 0;
            if (progress >= points.length - 1) {
                Log.e("demon-go-spell", "completed");
                completionListener.completed();
                resetProgress();
            }
        }
    }

    void render() {
        boolean flashing = mistakeFlashDuration > 0;
        if (flashing) {
            mistakeFlashDuration -= Gdx.graphics.getDeltaTime();
        }

        // start point
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(flashing ? Color.RED : progress > 0 || subProgress > 0 ? Color.GREEN : Color.WHITE);
        renderer.circle(points[0].x, points[0].y, WIDTH * 2);
        renderer.end();

        // draw uncompleted part
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(flashing ? Color.RED : Color.WHITE);
        for (int i = progress; i < points.length - 1; i++) {
            renderer.rectLine(points[i], points[i + 1], WIDTH);
        }
        renderer.end();

        // draw completed part
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(flashing ? Color.RED : Color.GREEN);
        for (int i = 0; i < progress; i++) {
            renderer.rectLine(points[i], points[i + 1], WIDTH);
        }
        // draw semi-completed part, if any
        if (subProgress > 0) {
            renderer.rectLine(points[progress],
                    points[progress].cpy().add(points[progress + 1].cpy().sub(points[progress]).scl(subProgress)),
                    WIDTH);
        }
        renderer.end();
    }
}
