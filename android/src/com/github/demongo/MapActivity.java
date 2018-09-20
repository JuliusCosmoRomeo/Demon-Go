package com.github.demongo;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.badlogic.gdx.math.Circle;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionBase;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionHeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionOpacity;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private MapboxMap mapboxMap;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Icon playerIcon;
    private Icon myPlayerIcon;

    private HashMap<String,Marker> playerMarkers = new HashMap<>();

    private PvP pvp;

    private boolean loadedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl("mapbox://styles/tom95/cji612zco1t3b2spbn7leib2q");

        playerIcon = IconFactory.getInstance(this).fromAsset("player_location.png");
        myPlayerIcon = IconFactory.getInstance(this).fromAsset("player_location.png");

        pvp = new PvP(this);
        pvp.setUpdatePositionCallback(new PvP.UpdatePositionCallback() {
            @Override
            public void updated(String id, GeoPoint point) {
                updatePlayerMarker(id, point);
                if (id.equals(pvp.getMyId()) && !loadedOnce) {
                    loadedOnce = true;
                    pvp.loadInArea(point, 1000);
                }
            }
        });

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap map) {
                mapboxMap = map;
                setupBuildings();
                fetchMarkers();
                mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        addStashMarker(point.getLatitude(), point.getLongitude());
                    }
                });
            }
        });
    }

    void fetchMarkers() {
        db.collection("stashes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null || snapshot == null) {
                    Log.w("demon-go-map", "Listen failed.", e);
                    return;
                }

                for (QueryDocumentSnapshot doc : snapshot) {
                    GeoPoint position = (GeoPoint) doc.get("position");
                    if (position != null)  {
                        addStashMarker(position.getLatitude(), position.getLongitude());
                    }
                }
            }
        });
    }

    private void updatePlayerMarker(String id, GeoPoint point) {
        if (playerMarkers.containsKey(id)) {
            Marker marker = playerMarkers.get(id);
            marker.setPosition(new LatLng(point.getLatitude(), point.getLongitude()));
            mapboxMap.updateMarker(marker);
            return;
        }

        Marker marker = addPlayerMarker(point.getLatitude(), point.getLongitude(), id.equals(pvp.getMyId()));
        playerMarkers.put(id, marker);
    }

    private Marker addPlayerMarker(double lat, double lng, boolean isMyPlayer) {
        LatLng pos = new LatLng(lat, lng);
        MarkerOptions marker = new MarkerOptions();
        marker.setIcon(isMyPlayer ? myPlayerIcon : playerIcon);
        marker.setPosition(pos);
        return mapboxMap.addMarker(marker);
    }

    private void addStashMarker(double lat, double lng) {
        LatLng pos = new LatLng(lat, lng);
        MarkerOptions marker = new MarkerOptions();
        marker.setIcon(IconFactory.getInstance(this).defaultMarker());
        marker.setPosition(pos);
        mapboxMap.addMarker(marker);

        List<LatLng> polygon = new ArrayList<>();
        polygon.add(MapUtils.move(pos, 0, 100));
        polygon.add(MapUtils.move(pos, 100, 0));
        polygon.add(MapUtils.move(pos, 0, -100));
        polygon.add(MapUtils.move(pos, -100,0));

        mapboxMap.addPolygon(new PolygonOptions().addAll(polygon).fillColor(Color.parseColor("#33ff0000")));
    }

    private void addNewMarker(double lat, double lng) {
        addStashMarker(lat, lng);

        Stash stash = new Stash(0,new GeoPoint(lat, lng),1000,0);

        db.collection("stashes").add(stash.getMap());

    }

    private void setupBuildings() {
        FillExtrusionLayer fillExtrusionLayer = new FillExtrusionLayer("3d-buildings", "composite");
        fillExtrusionLayer.setSourceLayer("building");
        fillExtrusionLayer.setFilter(eq(get("extrude"), "true"));
        fillExtrusionLayer.setMinZoom(15);
        fillExtrusionLayer.setProperties(
                fillExtrusionColor(Color.LTGRAY),
                fillExtrusionHeight(
                        interpolate(
                                exponential(1f),
                                zoom(),
                                stop(15, literal(0)),
                                stop(16, get("height"))
                        )
                ),
                fillExtrusionBase(get("min_height")),
                fillExtrusionOpacity(0.9f)
        );
        mapboxMap.addLayer(fillExtrusionLayer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_mark) {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
