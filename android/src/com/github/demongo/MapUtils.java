package com.github.demongo;

import com.google.firebase.firestore.GeoPoint;
import com.mapbox.mapboxsdk.geometry.LatLng;

public class MapUtils {

    // https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
    static LatLng move(LatLng source, double dx, double dy) {
        final double r_earth = 6371 * 1000;
        return new LatLng(
                source.getLatitude()  + (dy / r_earth) * (180.0 / Math.PI),
                source.getLongitude() + (dx / r_earth) * (180.0 / Math.PI) / Math.cos(source.getLatitude() * Math.PI/180.0)
        );
    }

    static GeoPoint move(GeoPoint source, double dx, double dy) {
        LatLng res = move(new LatLng(source.getLatitude(), source.getLongitude()), dx, dy);
        return new GeoPoint(res.getLatitude(), res.getLongitude());
    }
}
