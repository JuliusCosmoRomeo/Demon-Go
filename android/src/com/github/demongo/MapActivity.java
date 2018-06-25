package com.github.demongo;

import android.gesture.GestureOverlayView;
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
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.light.Position;

import java.util.ArrayList;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl("mapbox://styles/tom95/cji612zco1t3b2spbn7leib2q");

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap map) {
                mapboxMap = map;
                setupBuildings();
                fetchMarkers();
                mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        addNewMarker(point.getLatitude(), point.getLongitude());
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
                        addMarker(position.getLatitude(), position.getLongitude());
                    }
                }
            }
        });
    }

    private List<LatLng> getStashPerimeter(LatLng position, double radiusInKm) {

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
            Log.d("demon-go-map", x + " " + y);
            polygon.add(new LatLng(position.getLatitude() + y, position.getLongitude() + x));
        }

        return polygon;
    }

    private void addMarker(double lat, double lng) {
        LatLng pos = new LatLng(lat, lng);
        MarkerOptions marker = new MarkerOptions();
        marker.setPosition(pos);
        mapboxMap.addMarker(marker);

        /*List<LatLng> polygon = new ArrayList<>();
        polygon.add(MapUtils.move(pos, 0, 100));
        polygon.add(MapUtils.move(pos, 100, 0));
        polygon.add(MapUtils.move(pos, 0, -100));
        polygon.add(MapUtils.move(pos, -100,0));
*/
        List<LatLng> polygon = getStashPerimeter(pos,3);
        mapboxMap.addPolygon(new PolygonOptions().addAll(polygon).fillColor(Color.parseColor("#33ff0000")));
    }

    private void addNewMarker(double lat, double lng) {
        addMarker(lat, lng);

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
