package com.github.demongo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public class PvP {

    private Location location = null;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String myId;

    private UpdatePositionCallback callback = null;

    interface UpdatePositionCallback {
        void updated(String id, GeoPoint point);
    }

    String getMyId() {
        return myId;
    }

    PvP(Context context) {
        loadUserId(context);

        /*new LocationQuery(context, new LocationQuery.LocationFoundListener() {
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
        });*/
        new LocationStream(context, new LocationStream.LocationFoundListener() {
            @Override
            public void onFoundLocation(Location _location) {
                Log.e("demon-go-pvp", "Accepted location " + _location.getAccuracy());
                location = _location;
                GeoPoint point = new GeoPoint(location.getLongitude(), location.getLatitude());
                updatePosition(point);
                callback.updated(myId, point);
            }

            @Override
            public void onError(String msg) {
                // TODO
                Log.e("demon-go-pvp", "Location error: " + msg);
            }
        });
    }

    public void setUpdatePositionCallback(UpdatePositionCallback _callback) {
        callback = _callback;
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
        loadInArea(myLocation, 200);
    }

    public void loadInArea(GeoPoint myLocation, int sideDistance) {
        final GeoPoint upper = MapUtils.move(myLocation, sideDistance, sideDistance);
        final GeoPoint lower = MapUtils.move(myLocation, -sideDistance, -sideDistance);
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

                        if (callback != null)
                            callback.updated(doc.getId(), (GeoPoint) doc.get("position"));
                    }
                }
            });
    }

    public boolean isReady() {
        return location != null;
    }

}
