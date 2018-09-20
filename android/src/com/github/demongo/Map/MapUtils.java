package com.github.demongo.Map;

import android.support.annotation.NonNull;
import android.util.Log;

import com.github.demongo.Stash;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
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

    /*checks if the stash would intersect with other stashes
     - if a newly created stash lies in the radius of another returns -1
     - otherwise returns the max radius the stash could have

     */
    public static double getMaxRadius(Stash stash, ArrayList<Stash> stashes){
        double maxRadius = Double.MAX_VALUE;
        for (Stash otherStash : stashes){
            if (!otherStash.getId().toString().equals(stash.getId().toString())){

                double radius = getDistanceBw(stash, otherStash);
                if (radius==-1){
                    return -1;
                }
                if (radius < maxRadius){
                    maxRadius = radius;
                }
            }
        }

        return maxRadius;

    }

    //returns dist in km
    private static double getDistanceBw(Stash stash, Stash otherStash) {

        double lat1 = stash.getLocation().getLatitude();
        double lon1 = stash.getLocation().getLongitude();

        double lat2 = otherStash.getLocation().getLatitude();
        double lon2 = otherStash.getLocation().getLongitude();

        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        dist = rad2deg(dist);

        if (dist < otherStash.getRadius()){
            return -1;
        } else {
            //from the distance we need to substract the radius of the other stash
            dist -= otherStash.getRadius();

            return dist;
        }
    }

    //https://www.geodatasource.com/developers/java
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts decimal degrees to radians						 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::	This function converts radians to decimal degrees						 :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
