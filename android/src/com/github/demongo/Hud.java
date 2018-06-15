package com.github.demongo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

public class Hud {
    interface TriggerListener {
        void onPvPStarted();
    }

    private Stage hud;

    private Actor loading;

    private TriggerListener listener;

    Hud(final Context context, float scalingFactor, TriggerListener _listener) {
        listener = _listener;
        hud = new Stage(new ScalingViewport(Scaling.fit,
        Gdx.graphics.getWidth() / scalingFactor,
        Gdx.graphics.getHeight() / scalingFactor));

        float hudWidth = hud.getWidth();
        float hudHeight = hud.getHeight();
        Skin skin = new Skin(Gdx.files.internal("uiskin.json"));

        Gdx.input.setInputProcessor(hud);

        Button showMap = new TextButton("Show Map", skin);
        showMap.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                context.startActivity(new Intent(context, MapActivity.class));
            }
        });
        hud.addActor(showMap);

        final Button startPvP = new TextButton("Auf Ärger aus sein", skin);
        startPvP.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                listener.onPvPStarted();
                startPvP.setVisible(false);
            }
        });
        startPvP.setPosition(hudWidth - startPvP.getWidth(), 0);
        hud.addActor(startPvP);

        loading = new Label("Gathering Energy ...", skin);

        loading.setPosition(hudWidth / 2 - loading.getWidth() / 2, hudHeight / 2 - loading.getHeight() / 2);
        hud.addActor(loading);
    }

    public void setLoading(boolean _loading) {
        loading.setVisible(_loading);
    }

    public void draw() {
        hud.act(Gdx.graphics.getDeltaTime());
        hud.draw();
    }
}
