package com.github.demongo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public class PvP {


    private Location location = null;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String myId;
    private Hud hud;

    PvP(Context context, Hud _hud) {
        hud = _hud;
        loadUserId(context);

        new LocationQuery(context, new LocationQuery.LocationFoundListener() {
            @Override
            public void onError(String msg) {
                // TODO
            }

            @Override
            public void onFoundLocation(Location _location) {
                Log.e("demon-go-pvp", "Accepted location " + _location.getAccuracy());
                location = _location;

                GeoPoint point = new GeoPoint(location.getLongitude(), location.getLatitude());

                updatePosition(point);
                loadNearby(point);
            }
        });
    }

    private void updatePosition(GeoPoint point) {
        Map<String,Object> data = new HashMap<>();
        data.put("position", point);
        data.put("positionTime", Timestamp.now());
        db.collection("user").document(myId).set(data, SetOptions.merge());
    }

    public void updateCloudAnchorId(String id) {
        Map<String,Object> data = new HashMap<>();
        data.put("cloudAnchorId", id);
        db.collection("user").document(myId).set(data, SetOptions.merge());
    }

    private void leave() {
        updatePosition(null);
    }

    private void loadUserId(Context context) {
        SharedPreferences preferences = ((Activity) context).getPreferences(Context.MODE_PRIVATE);

        myId = preferences.getString("userid", null);
        if (myId == null) {
            myId  = UUID.randomUUID().toString();
            preferences.edit().putString("userid", myId).apply();
        }
    }

    private void loadNearby(GeoPoint myLocation) {
        final GeoPoint upper = MapUtils.move(myLocation, 200, 200);
        final GeoPoint lower = MapUtils.move(myLocation, -200, -200);
        final long now = Timestamp.now().getSeconds();

        Log.e("demon-go-pvp", "Looking for contenders");
        db.collection("user")
            .whereGreaterThanOrEqualTo("position", lower)
            .whereLessThan("position", upper)
            .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.e("demon-go-pvp", "--- reply received:" + upper.toString() + " " + lower.toString());
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Timestamp time = ((Timestamp) doc.get("positionTime"));
                    if (time != null && now - time.getSeconds() < 60 * 5) {
                        Log.e("demon-go-pvp", "Battling with recently seen " + doc.getId());
                    }
                    Log.e("demon-go-pvp", "contender: " + doc.getId() +" " + doc.get("position").toString());
                }
            }
        });
    }

    public boolean isReady() {
        return location != null;
    }

}
