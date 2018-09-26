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
    private static final int STEPS = 4;
    private static final float SCREEN_OFFSET = 100.0f;

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
        points = new Vector2[STEPS];

        float width = canvasWidth - SCREEN_OFFSET * 2;
        float height = canvasHeight - SCREEN_OFFSET * 2;

        for (int i = 0; i < STEPS; i++) {
            points[i] = new Vector2(
                SCREEN_OFFSET + (float) Math.random() * width,
                SCREEN_OFFSET + (float) Math.random() * height);
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
        final Color COMPLETED_COLOR = Color.GREEN;
        final Color ERROR_COLOR = new Color(1, 0, 0, 1);
        final Color UNCOMPLETED_COLOR = Color.WHITE;

        boolean flashing = mistakeFlashDuration > 0;
        if (flashing) {
            mistakeFlashDuration -= Gdx.graphics.getDeltaTime();
        }

        // start point
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(flashing ? ERROR_COLOR : progress > 0 || subProgress > 0 ? COMPLETED_COLOR : UNCOMPLETED_COLOR);
        renderer.circle(points[0].x, points[0].y, WIDTH * 2);
        renderer.end();

        // draw uncompleted part
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(flashing ? ERROR_COLOR : UNCOMPLETED_COLOR);
        for (int i = progress; i < points.length - 1; i++) {
            renderer.rectLine(points[i], points[i + 1], WIDTH);
        }
        renderer.end();

        // draw completed part
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(flashing ? ERROR_COLOR : COMPLETED_COLOR);
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
