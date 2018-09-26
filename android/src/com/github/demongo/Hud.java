package com.github.demongo;

import android.content.Context;
import android.content.Intent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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

    private TriggerListener listener;

    private Label debugInfo;
    private ARDebug debug;

    private static final float HP_INSET = 24.0f;
    private static final float HP_HEIGHT = 12.0f;
    private Actor hp;

    private boolean showSpellCanvas = false;

    Hud(final Context context, float scalingFactor, TriggerListener _listener, ARDebug arDebug) {
        listener = _listener;
        hud = new Stage(new ScalingViewport(Scaling.fit,
        Gdx.graphics.getWidth() / scalingFactor,
        Gdx.graphics.getHeight() / scalingFactor));

        debug = arDebug;

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
        // ar pvp is disabled atm
        // hud.addActor(startPvP);

        Label.LabelStyle rectStyle = new Label.LabelStyle(new Label("", skin).getStyle());
        Pixmap p = new Pixmap((int) hudWidth, (int) hudHeight, Pixmap.Format.RGB888);
        p.setColor(Color.RED);
        p.fill();
        rectStyle.background = new Image(new Texture(p)).getDrawable();
        hp = new Label("", rectStyle);
        hp.setColor(1, 0.0f, 0.0f, 1.0f);
        hp.setPosition(HP_INSET, hudHeight - HP_INSET);
        hp.setHeight(HP_HEIGHT);
        setDemonHealth(1.0f, false);
        hud.addActor(hp);

        debugInfo = new Label("", skin);
        debugInfo.setFontScale(0.3f);
        hud.addActor(debugInfo);
    }

    public void setDemonHealth(float healthPercent, boolean isPhaseTwo) {
        if (healthPercent <= 0) {
            hp.setVisible(false);
            return;
        }

        hp.setVisible(true);
        // hp.setColor(isPhaseTwo ? Color.GREEN : Color.RED);
        hp.setWidth((hud.getWidth() - HP_INSET * 2) * healthPercent);
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
        debugInfo.setText(debug.getInfoString());
        debugInfo.setPosition(0, hud.getHeight() - debugInfo.getPrefHeight() / 2);

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
