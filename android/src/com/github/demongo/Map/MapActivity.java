package com.github.demongo.Map;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.demongo.Demon;
import com.github.demongo.DemonBattle;
import com.github.demongo.DemonGallery;
import com.github.demongo.ParcelableGeoPoint;
import com.github.demongo.PlayerUtil;
import com.github.demongo.R;
import com.github.demongo.Stash;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.HashMap;
import java.util.UUID;

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
    private static final String TAG = "demon-go-map";
    public static final UUID playerId = UUID.fromString("1b9624f8-4683-41c6-823e-89932573aa67");
    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE = "custom-marker";
    static final int GALLERY_REQUEST = 1;  // The request code

    //here we store the markers for stashes and demons to remove them easily later
    private HashMap<ParcelUuid, Marker> stashMarkerMap;
    private HashMap<String, String> demonMarkerMap;
    private HashMap<ParcelUuid, PolygonOptions> stashPerimeterMap;

    private MapView mapView;
    private MapboxMap mapboxMap;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Stash currentStash;
    private int demonCount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl("mapbox://styles/tom95/cji612zco1t3b2spbn7leib2q");
        //clearAllStashes();
        stashMarkerMap = new HashMap();
        demonMarkerMap = new HashMap();
        stashPerimeterMap = new HashMap();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap map) {
                mapboxMap = map;
                //add the demon icon as resource to the map
                Bitmap icon = BitmapFactory.decodeResource(
                        MapActivity.this.getResources(), R.drawable.demon);
                mapboxMap.addImage(MARKER_IMAGE, icon);

                setupBuildings();
                fetchStashes();
                mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        Stash stash = new Stash(new ParcelUuid(UUID.randomUUID()), new ParcelUuid(playerId),
                                new ParcelableGeoPoint(new GeoPoint(point.getLatitude(), point.getLongitude())),
                                0,
                                1000,
                                0);
                        db.collection("stashes").document(stash.getId().toString()).set(stash.getMap());
                        //addNewStashMarker(stash);
                        //addDemonMarkers(new Demon("luschi",100,30,230, R.drawable.notification_icon,Demon.Type.Imp, stash.getId()),point);
                    }
                });

                addCustomInfoWindowAdapter();
            }
        });
    }

    private void clearAllStashes(){
        db.collection("stashes").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().collection("demons").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for (QueryDocumentSnapshot demon : task.getResult()) {
                                demon.getReference().delete();
                            }
                        }
                    });
                    document.getReference().delete();
                }
            }
        });
    }


    private void addCustomInfoWindowAdapter() {
        mapboxMap.setInfoWindowAdapter(new MapboxMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(@NonNull final Marker marker) {
                StashMarker stashMarker = (StashMarker) marker;
                final Stash stash = stashMarker.getStash();
                View container = getLayoutInflater().inflate(R.layout.map_info_window, null);
                TextView playerName = container.findViewById(R.id.playerName);
                playerName.setText("Player 1");
                TextView radius = container.findViewById(R.id.radius);
                radius.setText("Radius:" + stash.getRadius()+ " km");
                TextView capacity = container.findViewById(R.id.capacity);
                capacity.setText(stash.getFilled() +"/" +stash.getCapacity() + " EP");
                LinearLayout buttonContainer = container.findViewById(R.id.buttonContainer);
                if (PlayerUtil.isCurrentPlayer(stash.getPlayerID(),playerId)){
                    ImageButton defendBtn = new ImageButton(MapActivity.this);
                    defendBtn.setImageResource(R.drawable.icons8_schild);
                    defendBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent demonGallery = new Intent(MapActivity.this,DemonGallery.class);
                            demonGallery.putExtra("action", DemonGallery.Action.Defend);
                            currentStash = stash;
                            marker.hideInfoWindow();
                            startActivityForResult(demonGallery,GALLERY_REQUEST);
                        }
                    });
                    buttonContainer.addView(defendBtn);
                    ImageButton depositBtn = new ImageButton(MapActivity.this);
                    depositBtn.setImageResource(R.drawable.icons8_gelddose);
                    depositBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showDepositPopup(stash, marker);
                        }
                    });
                    buttonContainer.addView(depositBtn);
                } else {
                    ImageButton attackBtn = new ImageButton(MapActivity.this);
                    attackBtn.setImageResource(R.drawable.icons8_schwert);
                    attackBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent demonGallery = new Intent(MapActivity.this,DemonGallery.class);
                            demonGallery.putExtra("action", DemonGallery.Action.Attack);
                            currentStash = stash;
                            marker.hideInfoWindow();
                            startActivityForResult(demonGallery,GALLERY_REQUEST);
                        }
                    });
                    buttonContainer.addView(attackBtn);
                }
                ImageButton deleteBtn = new ImageButton(MapActivity.this);
                deleteBtn.setImageResource(R.drawable.delete_icon);
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentStash = stash;
                        Log.i(TAG, "deleting stash " +stash.getId().toString());
                        db.collection("stashes").document(stash.getId().toString()).delete();
                        db.collection(currentStash.getId().toString()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Stash st = new Stash(document.getData());
                                    Log.i(TAG, "stash still exists " + st.toString());
                                }
                            }
                        });
                        marker.hideInfoWindow();
                    }
                });
                buttonContainer.addView(deleteBtn);

                return container;
            }
        });
    }

    void fetchStashes() {
        db.collection("stashes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null || snapshot == null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                Stash stash;
                QueryDocumentSnapshot doc;
                for (DocumentChange dc : snapshot.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.i(TAG, "New stash: " + dc.getDocument().getData());
                            doc = dc.getDocument();
                            stash = new Stash(doc.getData());
                            addStashMarkerWithDemons(stash, doc);
                            break;
                        case MODIFIED:
                            Log.i(TAG, "Modified stash: " + dc.getDocument().getData());
                            stash = new Stash(dc.getDocument().getData());
                            updateStashMarker(stash);
                            break;
                        case REMOVED:
                            Log.i(TAG, "Removed stash: " + dc.getDocument().getData());
                            stash = new Stash(dc.getDocument().getData());
                            removeStashMarkerWithDemons(stash);
                            break;
                    }
                }
            }
        });
    }

    private void addStashMarkerWithDemons(Stash stash, QueryDocumentSnapshot doc) {
        GeoPoint position = stash.getLocation();
        if (position != null)  {
            boolean isCurrentPlayer = PlayerUtil.isCurrentPlayer(stash.getPlayerID(),playerId);
            if (stash.getRadius()==-1){
                stash.setRadius(1);
            }
            addStashMarker(stash, isCurrentPlayer);

            //the current player can only see his own stashes
            if(isCurrentPlayer){
                fetchDemonsForStash(doc, position);
            }
        }
    }

    private void updateStashMarker(Stash stash){
        removeStashMarker(stash.getId());
        boolean isCurrentPlayer = PlayerUtil.isCurrentPlayer(stash.getPlayerID(),playerId);
        if (stash.getRadius()==-1){
            stash.setRadius(1);
        }
        addStashMarker(stash, isCurrentPlayer);
    }

    private void removeStashMarkerWithDemons(Stash stash){
        removeStashMarker(stash.getId());

        db.collection("stashes").document(stash.getId().toString()).collection("demons").get().addOnCompleteListener(
            new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            }
        );
    }


    private void fetchDemonsForStash(QueryDocumentSnapshot doc, final GeoPoint position){
        db.collection("stashes").document(doc.getId()).collection("demons").addSnapshotListener(
            new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot snapshot,
                                    @Nullable FirebaseFirestoreException e) {
                    if (e != null || snapshot == null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    for (DocumentChange dc : snapshot.getDocumentChanges()) {
                        Demon demon;
                        QueryDocumentSnapshot document = dc.getDocument();
                        switch (dc.getType()) {
                            case REMOVED:
                                //added and removed are same for demon marker updates
                            case ADDED:
                                Log.i(TAG, "adding demon " + document.getId() + " to stash " + doc.getId().toString());
                                ArrayList<Demon> demons = new ArrayList<>();
                                db.collection("stashes").document(doc.getId().toString()).collection("demons").get().addOnCompleteListener(
                                    new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                Demon defender = new Demon(document.getData());
                                                demons.add(defender);
                                                addDemonMarkers(demons,new LatLng(position.getLatitude(), position.getLongitude()), doc.getId());
                                            }
                                        }
                                    }
                                );

                                break;
                            case MODIFIED:
                                break;
                        }
                    }
                }
            }
        );
    }


    private void addStashMarker(Stash stash, boolean isCurrentPlayer) {
        LatLng pos = new LatLng(stash.getLocation().getLatitude(), stash.getLocation().getLongitude());
        StashMarkerOptions markerOptions = new StashMarkerOptions(stash);
        markerOptions.setPosition(pos);
        Marker marker = mapboxMap.addMarker(markerOptions);
        stashMarkerMap.put(stash.getId(),marker);


        List<LatLng> polygon = MapUtils.getStashPerimeter(pos,stash.getRadius());
        String colorString = isCurrentPlayer ?  "#00ff3300" : "#33ff0000";
        PolygonOptions polygonOpts = new PolygonOptions().addAll(polygon).fillColor(Color.parseColor(colorString));
        stashPerimeterMap.put(stash.getId(),polygonOpts);
        mapboxMap.addPolygon(polygonOpts);
    }

    private void removeStashMarker(ParcelUuid uuid){
        mapboxMap.removeMarker(stashMarkerMap.get(uuid));
        mapboxMap.removePolygon(stashPerimeterMap.get(uuid).getPolygon());

        stashMarkerMap.remove(uuid);
        stashPerimeterMap.remove(uuid);
    }

    private void addDemonMarkers(ArrayList<Demon> demons, LatLng target, String stashId) {
        if (demonMarkerMap.get(stashId)!=null) {
            mapboxMap.removeLayer(demonMarkerMap.get(stashId));
        }
        if(demons.size()==0){
            return;
        }
        List<Feature> features = new ArrayList<>();

        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
        if (demons.size()==1){
            LatLng point = MapUtils.move(target,-50,0);
            features.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
        } else if (demons.size()==2){
            LatLng point = MapUtils.move(target,-50,0);
            LatLng point2 = MapUtils.move(target,50,0);
            features.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point2.getLongitude(),point2.getLatitude())));
        } else if (demons.size()==3){
            LatLng point = MapUtils.move(target,-40,0);
            LatLng point2 = MapUtils.move(target,33.54,-33.54);
            LatLng point3 = MapUtils.move(target,33.54,33.54);
            features.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point2.getLongitude(),point2.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point3.getLongitude(),point3.getLatitude())));
        } else if (demons.size()==4){
            LatLng point = MapUtils.move(target,-33,-33);
            LatLng point2 = MapUtils.move(target,33,-33);
            LatLng point3 = MapUtils.move(target,33,33);
            LatLng point4 = MapUtils.move(target,-33,33);
            features.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point2.getLongitude(),point2.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point3.getLongitude(),point3.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point4.getLongitude(),point4.getLatitude())));
        } else if (demons.size()>4) {
            LatLng point = MapUtils.move(target, -33, -33);
            LatLng point2 = MapUtils.move(target, 33, -33);
            LatLng point3 = MapUtils.move(target, 33, 33);
            LatLng point4 = MapUtils.move(target, -33, 33);
            features.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point2.getLongitude(), point2.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point3.getLongitude(), point3.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(point4.getLongitude(), point4.getLatitude())));
            features.add(Feature.fromGeometry(Point.fromLngLat(target.getLongitude(), target.getLatitude())));
        }
        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
        GeoJsonSource source = new GeoJsonSource(MARKER_SOURCE + demonCount, featureCollection);

        mapboxMap.addSource(source);

        /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
        SymbolLayer markerStyleLayer = new SymbolLayer(MARKER_STYLE_LAYER + demonCount, MARKER_SOURCE + demonCount)
                .withProperties(
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconImage(MARKER_IMAGE)
                );
        mapboxMap.addLayer(markerStyleLayer);
        demonMarkerMap.put(stashId,markerStyleLayer.getId());
        demonCount++;
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


    public void showDepositPopup(final Stash stash, final Marker marker){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout dialogView =  (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_deposit_stash,null);
        final SeekBar slider = dialogView.findViewById(R.id.deposit_slider);
        slider.setMax((int)(stash.getCapacity()));
        slider.setProgress((int)stash.getFilled());
        final TextView depositText = dialogView.findViewById(R.id.deposit_value);
        depositText.setText("" + stash.getFilled());
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                depositText.setText(progress + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        builder.setMessage("Wie viel EP möchtest du im Versteck ablegen?")
                .setTitle("Put into stash")
                .setView(dialogView)
                .setPositiveButton("Deposit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //deposit value to stash
                        marker.hideInfoWindow();
                        stash.setFilled(slider.getProgress());
                        db.collection("stashes").document(stash.getId().toString()).set(stash.getMap()).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot successfully written! ");
                            }
                        })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error writing document", e);
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "canceled");
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
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

    private void onAttack(Demon demon){
        db.collection("stashes").document(currentStash.getId().toString()).collection("demons").get().addOnCompleteListener(
            new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    ArrayList<Demon> defenders = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Demon defender = new Demon(document.getData());
                            defenders.add(defender);
                            Log.i(TAG, "defending demon " + defender.toString());
                        }
                        DemonBattle.attackStash(demon,defenders,currentStash);
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            }
        );
    }

    private void onDefend(Demon demon){
        //addDemonMarkers(demon,new LatLng(currentStash.getLocation().getLatitude(), currentStash.getLocation().getLongitude()));
        db.collection("stashes").document(currentStash.getId().toString()).collection("demons").document(demon.getId().toString()).set(demon.getMap());
        db.collection("stashes").document(currentStash.getId().toString()).collection("demons").get().addOnCompleteListener(
            new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    long totalDefenderHP = 0;
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Demon defender = new Demon(document.getData());
                            totalDefenderHP += defender.getHp();
                        }
                        double radius = (double)totalDefenderHP/1000;

                        currentStash.setRadius(radius);
                        Log.i(TAG, "stash before update " + currentStash.toString());
                        db.collection("stashes").document(currentStash.getId().toString()).set(currentStash.getMap());
                    } else {
                        Log.d(TAG, "Error getting documents: ", task.getException());
                    }
                }
            }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GALLERY_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Demon demon = data.getParcelableExtra("demon");
                DemonGallery.Action action = (DemonGallery.Action) data.getExtras().get("action");
                Log.i(TAG,demon.toString());
                // The user picked a demon.
                switch(action){
                    case Attack:
                        onAttack(demon);
                        break;
                    case Defend:
                        onDefend(demon);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
