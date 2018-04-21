package com.github.demongo;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.claywilkinson.arcore.gdx.BaseARCoreActivity;
import com.github.demongo.DemonGoGame;

public class AndroidLauncher extends BaseARCoreActivity {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useImmersiveMode = true;
		initialize(new DemonGoGame(), config);
	}
}
