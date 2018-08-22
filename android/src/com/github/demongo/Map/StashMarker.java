package com.github.demongo.Map;

import com.github.demongo.Stash;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Marker;

public class StashMarker extends Marker {

    private Stash stash;

    public StashMarker(BaseMarkerOptions baseMarkerOptions, Stash stash) {
        super(baseMarkerOptions);
        this.stash = stash;
    }

    public Stash getStash() {
        return stash;
    }
}
