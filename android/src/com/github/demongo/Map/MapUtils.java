package com.github.demongo.Map;

import com.google.firebase.firestore.GeoPoint;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MapUtils {

    // https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
    public static LatLng move(LatLng source, double dx, double dy) {
        final double r_earth = 6371 * 1000;
        return new LatLng(
                source.getLatitude()  + (dy / r_earth) * (180.0 / Math.PI),
                source.getLongitude() + (dx / r_earth) * (180.0 / Math.PI) / Math.cos(source.getLatitude() * Math.PI/180.0)
        );
    }

    public static GeoPoint move(GeoPoint source, double dx, double dy) {
        LatLng res = move(new LatLng(source.getLatitude(), source.getLongitude()), dx, dy);
        return new GeoPoint(res.getLatitude(), res.getLongitude());
    }


    public static List<LatLng> getStashPerimeter(LatLng position, double radiusInKm) {

        final int numberOfSides = 64;
        // these are conversion constants
        final double distanceX = radiusInKm / (111.319 * Math.cos(position.getLatitude() * Math.PI / 180));
        final double distanceY = radiusInKm / 110.574;

        double slice = (2 * Math.PI) / numberOfSides;


        double theta, x,y;
        List<LatLng> polygon = new ArrayList<>();
        for (int i=0;i<numberOfSides;i++) {
            theta = i * slice;
            x = distanceX * Math.cos(theta);
            y = distanceY * Math.sin(theta);
            polygon.add(new LatLng(position.getLatitude() + y, position.getLongitude() + x));
        }

        return polygon;
    }
}
