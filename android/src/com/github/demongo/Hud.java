package com.github.demongo;

import android.content.Context;
import android.content.Intent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.github.demongo.Map.MapActivity;

public class Hud {
    interface TriggerListener {
        void onPvPStarted();

        void onSpellCompleted();
    }

    private Stage hud;

    private Actor loading;
    private Actor demonCaught;

    private TriggerListener listener;

    private boolean showSpellCanvas = false;

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
        // pvp is disabled atm
        // hud.addActor(startPvP);

        loading = new Label("Gathering Energy ...", skin);

        loading.setPosition(hudWidth / 2 - loading.getWidth() / 2, hudHeight / 2 - loading.getHeight() / 2);
        hud.addActor(loading);
    }

    public void setLoading(boolean _loading) {
        loading.setVisible(_loading);
    }

    public void showSpell() {
        showSpellCanvas = true;
        spellCanvas.newSpell();
    }

    private SpellCanvas spellCanvas = new SpellCanvas(Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            new SpellCanvas.CompletionListener() {
                @Override
                public void completed() {
                    listener.onSpellCompleted();
                    showSpellCanvas = false;
                }
            });

    public void draw() {
        hud.act(Gdx.graphics.getDeltaTime());
        hud.draw();

        if (showSpellCanvas) {
            spellCanvas.render();

            if (Gdx.input.isTouched()) {
                spellCanvas.checkProgress();
            } else {
                spellCanvas.resetProgress();
            }
        }
    }
}
