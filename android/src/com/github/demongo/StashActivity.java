package com.github.demongo;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

public class StashActivity extends Activity {
    private final static String TAG = "demon-go-stash";
    Stash stash;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stash);
        stash = (Stash) getIntent().getParcelableExtra("stash");
        Log.i(TAG, stash.toString());
    }

}
