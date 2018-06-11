package com.github.demongo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

public class Hud {
    private final static int PREF_HUDWIDTH = 640;
    private final static int PREF_HUDHEIGHT = 480;

    Stage hud;
    float hudWidth;
    float hudHeight;
    Skin skin;

    Hud(final Context context) {
        hud = new Stage(new ScalingViewport(Scaling.fit, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        hudWidth = hud.getWidth();
        hudHeight = hud.getHeight();
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Gdx.input.setInputProcessor(hud);

        Button showMap = new TextButton("Show Map", skin);
        showMap.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Log.e("demon-go-hud", "blah blub");
                context.startActivity(new Intent(context, MapActivity.class));
            }
        });

        /*showMap.addListener(new InputListener(){
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                Log.e("demon-go-hud", "hi");
                context.startActivity(new Intent(context, MapActivity.class));
                return true;
            }
        });*/
        hud.addActor(showMap);
    }

    public void draw() {
        hud.act(Gdx.graphics.getDeltaTime());
        hud.draw();
    }
}
