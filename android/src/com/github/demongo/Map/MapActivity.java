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

import com.github.demongo.AndroidLauncher;
import com.github.demongo.Demon;
import com.github.demongo.DemonBattle;
import com.github.demongo.DemonGallery;
import com.github.demongo.ParcelableGeoPoint;
import com.github.demongo.PlayerUtils;
import com.github.demongo.R;
import com.github.demongo.Stash;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

import static com.github.demongo.Demon.Type.Afrit;
import static com.github.demongo.Demon.Type.Djinn;
import static com.github.demongo.Demon.Type.Foliot;
import static com.github.demongo.Demon.Type.Imp;
import static com.github.demongo.Demon.Type.Marid;
import static com.github.demongo.DemonGallery.nullStashId;
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

import com.github.demongo.Demon.Type;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "demon-go-map";
    public static final UUID playerId = UUID.fromString("1b9624f8-4683-41c6-823e-89932573aa67");
    public static final UUID opponentId = UUID.fromString("1b9624f8-0000-41c6-823e-89932573aa67");
    private static final String MARKER_SOURCE = "markers-source";
    private static final String MARKER_STYLE_LAYER = "markers-style-layer";
    private static final String MARKER_IMAGE_IMP = "marker_imp";
    private static final String MARKER_IMAGE_FOLIOT = "marker_foliot";
    private static final String MARKER_IMAGE_DJINN = "marker_djinn";
    private static final String MARKER_IMAGE_AFRIT = "marker_afrit";
    private static final String MARKER_IMAGE_MARID = "marker_marid";
    static final int GALLERY_REQUEST = 1;  // The request code

    private DemonBattle battle;

    //here we store the markers for stashes and demons to remove them easily later
    private HashMap<ParcelUuid, Marker> stashMarkerMap;
    private HashMap<String, ArrayList<String>> demonMarkerMap;
    private HashMap<ParcelUuid, PolygonOptions> stashPerimeterMap;

    private MapView mapView;
    private MapboxMap mapboxMap;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Stash currentStash;
    private int demonLayerCount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if we were opened from the AR component after a successful demon heist!
        if (getIntent().hasExtra("demon-captured")) {
            handleDemonCaptured();
        }

        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.setStyleUrl("mapbox://styles/tom95/cji612zco1t3b2spbn7leib2q");

        battle = new DemonBattle(this,db);

        fetchPlayerEP();

        db.collection("players").document(playerId.toString()).set(new HashMap<String,Object>(){{
            put("ep",10000);
        }});

        resetPlayerDemons();

        stashMarkerMap = new HashMap();
        demonMarkerMap = new HashMap();
        stashPerimeterMap = new HashMap();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap map) {
                mapboxMap = map;

                //add the demon icons as resources to the map
                for (String demonName : Arrays.asList(MARKER_IMAGE_IMP,MARKER_IMAGE_FOLIOT,MARKER_IMAGE_DJINN,MARKER_IMAGE_AFRIT,MARKER_IMAGE_MARID)){
                    int drawable = -1;
                    if (demonName == MARKER_IMAGE_IMP){
                        drawable = R.drawable.imp;
                    } else if (demonName == MARKER_IMAGE_FOLIOT){
                        drawable = R.drawable.imp;
                    } else if (demonName == MARKER_IMAGE_DJINN){
                        drawable = R.drawable.djinn;
                    } else if (demonName == MARKER_IMAGE_AFRIT){
                        drawable = R.drawable.djinn;
                    } else if (demonName == MARKER_IMAGE_MARID){
                        drawable = R.drawable.djinn;
                    }
                    Bitmap icon = BitmapFactory.decodeResource(
                            MapActivity.this.getResources(), drawable);
                    mapboxMap.addImage(demonName, icon);
                }


                setupBuildings();
                fetchStashes();
                mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        final Stash stash = new Stash(new ParcelUuid(UUID.randomUUID()), new ParcelUuid(playerId),
                                new ParcelableGeoPoint(new GeoPoint(point.getLatitude(), point.getLongitude())),
                                0,
                                1000,
                                0,
                                false);
                        db.collection("stashes").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                ArrayList<Stash> stashes = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    stashes.add(new Stash(document.getData()));
                                }
                                if (MapUtils.getMaxRadius(stash,stashes) != -1){
                                    db.collection("stashes").document(stash.getId().toString()).set(stash.getMap());
                                }
                            }
                        });
                    }
                });
                addCustomInfoWindowAdapter();
            }
        });
    }

    private void handleDemonCaptured() {
        new NameDialog(this, new NameDialogChosenListener() {
            @Override
            public void nameChosen(String name) {
                Demon demon = new Demon(name, 100, 100, 30, 230, R.drawable.notification_icon, Demon.Type.Imp, nullStashId, new ParcelUuid(UUID.randomUUID()));
                db.collection("stashes").document(nullStashId.toString()).collection("demons").add(demon.getMap());

                Intent demonGallery = new Intent(MapActivity.this,DemonGallery.class);
                startActivityForResult(demonGallery, GALLERY_REQUEST);
            }
        });
    }

    private void fetchPlayerEP(){
        db.collection("players").document(playerId.toString()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot snapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {

                long ep = (long) snapshot.getData().get("ep");
                TextView epLabel = findViewById(R.id.epLabel);
                epLabel.setText(ep + " EP");
            }
        });
    }

    private void resetPlayerDemons(){

        db.collection("stashes").document(nullStashId.toString()).collection("demons").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                for (QueryDocumentSnapshot demon : task.getResult()) {
                    demon.getReference().delete();
                }

                ArrayList<Demon> demons = new ArrayList<Demon>(){{
                    add(new Demon("flupsi",100,100,30,230, R.drawable.notification_icon,Demon.Type.Imp, nullStashId, new ParcelUuid(UUID.randomUUID())));
                    add(new Demon("schnucksi",150,150,60,780, R.drawable.notification_icon,Demon.Type.Djinn, nullStashId, new ParcelUuid(UUID.randomUUID())));
                    add(new Demon("blubsi",220,220,90,1440, R.drawable.notification_icon,Demon.Type.Afrit, nullStashId, new ParcelUuid(UUID.randomUUID())));
                }};
                for(Demon demon : demons){
                    db.collection("stashes").document(nullStashId.toString()).collection("demons").document(demon.getId().toString()).set(demon.getMap());
                }
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

                final boolean isCurrentPlayer = PlayerUtils.isCurrentPlayer(stash.getPlayerID(),playerId);
                String player = isCurrentPlayer ? "Demon-Hunter" : "Gegner";
                playerName.setText("Besitzer: " + player);

                TextView radius = container.findViewById(R.id.radius);
                radius.setText("Radius: " + (int)(stash.getRadius() * 1000) + " m");

                TextView capacity = container.findViewById(R.id.capacity);
                String fillStatus = isCurrentPlayer ? stash.getFilled() +"/" +stash.getCapacity() + " EP" : stash.getFilled() + " EP";
                capacity.setText(fillStatus);

                LinearLayout buttonContainer = container.findViewById(R.id.buttonContainer);

                if (PlayerUtils.isCurrentPlayer(stash.getPlayerID(),playerId)){
                    ImageButton defendBtn = new ImageButton(MapActivity.this);
                    defendBtn.setImageResource(R.drawable.icons8_schild);
                    defendBtn.setBackground(null);
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
                    depositBtn.setBackground(null);
                    depositBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showDepositPopup(stash, marker);
                        }
                    });
                    buttonContainer.addView(depositBtn);

                    if (!stash.hasDefenders() && stash.getFilled()!=0){
                        ImageButton stealBtn = new ImageButton(MapActivity.this);
                        stealBtn.setImageResource(R.drawable.icons8_muenzen);
                        stealBtn.setBackground(null);
                        stealBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //player.setEP(+=stash.getFilled);
                                PlayerUtils.clearStash(currentStash,db);
                                marker.hideInfoWindow();

                            }
                        });
                        buttonContainer.addView(stealBtn);
                    }
                } else {
                    if (!stash.hasDefenders() && stash.getFilled()!=0){
                        ImageButton stealBtn = new ImageButton(MapActivity.this);
                        stealBtn.setImageResource(R.drawable.icons8_muenzen);
                        stealBtn.setBackground(null);
                        stealBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PlayerUtils.clearStash(currentStash,db);
                                marker.hideInfoWindow();
                            }
                        });
                        buttonContainer.addView(stealBtn);
                    } else {
                        ImageButton attackBtn = new ImageButton(MapActivity.this);
                        attackBtn.setImageResource(R.drawable.icons8_schwert);
                        attackBtn.setBackground(null);
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
                }

                ImageButton deleteBtn = new ImageButton(MapActivity.this);
                deleteBtn.setImageResource(R.drawable.delete_icon);
                deleteBtn.setBackground(null);
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

                ImageButton changePlayerBtn = new ImageButton(MapActivity.this);
                changePlayerBtn.setImageResource(R.drawable.icons8_wechsel);
                changePlayerBtn.setBackground(null);
                changePlayerBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentStash = stash;
                        if (isCurrentPlayer){
                            currentStash.setPlayerID(new ParcelUuid(opponentId));
                        } else {
                            currentStash.setPlayerID(new ParcelUuid(playerId));
                        }
                        db.collection("stashes").document(currentStash.getId().toString()).set(currentStash.getMap());
                    }
                });
                buttonContainer.addView(changePlayerBtn);

                return container;
            }
        });
    }

    void fetchStashes() {
        db.collection("stashes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null || snapshot == null){
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }
                Stash stash;
                QueryDocumentSnapshot doc;
                for (DocumentChange dc : snapshot.getDocumentChanges()) {
                    switch (dc.getType()){
                        case ADDED:
                            Log.i(TAG, "New stash: " + dc.getDocument().getData() + " (" + dc.getDocument().getId() + ")");
                            doc = dc.getDocument();
                            if (!doc.contains("player_id") || !(doc.get("player_id") instanceof String)) {
                                Log.i(TAG, "Skipping invalid document " + doc.getId());
                                continue;
                            }
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
            boolean isCurrentPlayer = PlayerUtils.isCurrentPlayer(stash.getPlayerID(),playerId);
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
        boolean isCurrentPlayer = PlayerUtils.isCurrentPlayer(stash.getPlayerID(),playerId);
        Log.i(TAG, "stash player id " + stash.getPlayerID().toString());
        Log.i(TAG, "is current player " + isCurrentPlayer);
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


    private void fetchDemonsForStash(final QueryDocumentSnapshot doc, final GeoPoint position){
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
                        switch (dc.getType()) {
                            case REMOVED:
                                //added and removed are same for demon marker updates
                            case ADDED:
                                final ArrayList<Demon> demons = new ArrayList<>();
                                db.collection("stashes").document(doc.getId().toString()).collection("demons").get().addOnCompleteListener(
                                    new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                Demon defender = new Demon(document.getData());
                                                demons.add(defender);
                                            }
                                            addDemonMarkers(demons,new LatLng(position.getLatitude(), position.getLongitude()), doc.getId());
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


    private void addStashMarker(Stash stash, boolean isCurrentPlayer){

        //we don't want to display empty (new) stashes (of others) with no defenders
        if (stash.getFilled()!=0 || stash.hasDefenders() || isCurrentPlayer){
            LatLng pos = new LatLng(stash.getLocation().getLatitude(), stash.getLocation().getLongitude());
            StashMarkerOptions markerOptions = new StashMarkerOptions(stash);
            markerOptions.setPosition(pos);
            Marker marker = mapboxMap.addMarker(markerOptions);
            stashMarkerMap.put(stash.getId(),marker);

            String colorString;
            double radius;

            if (!stash.hasDefenders()){
                //there are still EP lying in the stash that can be collected by everyone
                colorString = "#990000ff";
                radius = 0.1;
            } else {
                colorString = isCurrentPlayer ?  "#00ff3300" : "#33ff0000";
                radius = stash.getRadius();
            }
            if (stash.getFilled()!=0 || stash.hasDefenders()){
                List<LatLng> polygon = MapUtils.getStashPerimeter(pos,radius);
                PolygonOptions polygonOpts = new PolygonOptions().addAll(polygon).fillColor(Color.parseColor(colorString));
                stashPerimeterMap.put(stash.getId(),polygonOpts);
                mapboxMap.addPolygon(polygonOpts);
            }
        }
    }

    private void removeStashMarker(ParcelUuid uuid){
        Log.i(TAG, "removing");
        if (stashMarkerMap.containsKey(uuid)) {
            Log.i(TAG, "in map");
            mapboxMap.removeMarker(stashMarkerMap.get(uuid));
        }
        Log.i(TAG, "removing");
        if (stashPerimeterMap.containsKey(uuid)){
            Log.i(TAG, "in map");
            mapboxMap.removePolygon(stashPerimeterMap.get(uuid).getPolygon());
        }

        stashMarkerMap.remove(uuid);
        stashPerimeterMap.remove(uuid);
    }

    private void addDemonMarkers(ArrayList<Demon> demons, LatLng target, String stashId) {
        if (demonMarkerMap.get(stashId)!=null) {
            Log.i(TAG, "removing layer");
            ArrayList<String> demonMarkers = demonMarkerMap.get(stashId);
            for (String marker : demonMarkers){
                mapboxMap.removeLayer(marker);
            }

        }
        if(demons.size()==0){
            return;
        }
        List<Feature> impFeatures = new ArrayList<>();
        List<Feature> foliotFeatures = new ArrayList<>();
        List<Feature> djinnFeatures = new ArrayList<>();
        List<Feature> afritFeatures = new ArrayList<>();
        List<Feature> maridFeatures = new ArrayList<>();

        List<LatLng> points = new ArrayList<>();
        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
        if (demons.size()==1){
            LatLng point = MapUtils.move(target,-50,0);
            points.add(point);
        } else if (demons.size()==2){
            LatLng point = MapUtils.move(target,-50,0);
            LatLng point2 = MapUtils.move(target,50,0);
            points.add(point);
            points.add(point2);
        } else if (demons.size()==3){
            LatLng point = MapUtils.move(target,-40,0);
            LatLng point2 = MapUtils.move(target,33.54,-33.54);
            LatLng point3 = MapUtils.move(target,33.54,33.54);
            points.add(point);
            points.add(point2);
            points.add(point3);
        } else if (demons.size()==4){
            LatLng point = MapUtils.move(target,-33,-33);
            LatLng point2 = MapUtils.move(target,33,-33);
            LatLng point3 = MapUtils.move(target,33,33);
            LatLng point4 = MapUtils.move(target,-33,33);
            points.add(point);
            points.add(point2);
            points.add(point3);
            points.add(point4);
        } else if (demons.size()>4) {
            LatLng point = MapUtils.move(target, -33, -33);
            LatLng point2 = MapUtils.move(target, 33, -33);
            LatLng point3 = MapUtils.move(target, 33, 33);
            LatLng point4 = MapUtils.move(target, -33, 33);
            points.add(point);
            points.add(point2);
            points.add(point3);
            points.add(point4);
            points.add(target);
        }

        //for every demon type add a different icon
        for (int demon = 0; demon < demons.size(); demon++){
            if (demon < 5){
                Demon d = demons.get(demon);
                LatLng point = points.get(demon);
                switch(d.getType()){
                    case Imp:
                        impFeatures.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
                        break;
                    case Foliot:
                        foliotFeatures.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
                        break;
                    case Djinn:
                        djinnFeatures.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
                        break;
                    case Afrit:
                        afritFeatures.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
                        break;
                    case Marid:
                        maridFeatures.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(),point.getLatitude())));
                        break;
                    default:
                        break;
                }
            }
        }

        ArrayList<String> layerIds = new ArrayList<>();
        for (Type demonType : Arrays.asList(Imp, Foliot, Djinn, Afrit, Marid)){
            FeatureCollection featureCollection = null;
            String markerImage = "";
            switch(demonType){
                case Imp:
                    featureCollection = FeatureCollection.fromFeatures(impFeatures);
                    markerImage = MARKER_IMAGE_IMP;
                    break;
                case Foliot:
                    featureCollection = FeatureCollection.fromFeatures(foliotFeatures);
                    markerImage = MARKER_IMAGE_FOLIOT;
                    break;
                case Djinn:
                    featureCollection = FeatureCollection.fromFeatures(djinnFeatures);
                    markerImage = MARKER_IMAGE_DJINN;
                    break;
                case Afrit:
                    featureCollection = FeatureCollection.fromFeatures(afritFeatures);
                    markerImage = MARKER_IMAGE_AFRIT;
                    break;
                case Marid:
                    featureCollection = FeatureCollection.fromFeatures(maridFeatures);
                    markerImage = MARKER_IMAGE_MARID;
                    break;
                default:
                    break;
            }
            GeoJsonSource source = new GeoJsonSource(MARKER_SOURCE + demonLayerCount, featureCollection);
            mapboxMap.addSource(source);

            /* Style layer: A style layer ties together the source and image and specifies how they are displayed on the map. */
            SymbolLayer markerStyleLayer = new SymbolLayer(MARKER_STYLE_LAYER + demonLayerCount, MARKER_SOURCE + demonLayerCount)
                    .withProperties(
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconImage(markerImage)
                    );
            mapboxMap.addLayer(markerStyleLayer);
            layerIds.add(markerStyleLayer.getId());
            demonLayerCount++;
        }
        demonMarkerMap.put(stashId,layerIds);
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
        builder.setMessage("Wie viel EP möchtest du ablegen?")
                .setTitle("Put into stash")
                .setView(dialogView)
                .setPositiveButton("Deposit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //deposit value to stash
                        marker.hideInfoWindow();
                        PlayerUtils.updateEp(stash,db,-(slider.getProgress()-stash.getFilled()));
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
        if (item.getItemId() == R.id.action_seek) {
            startActivity(new Intent(this, AndroidLauncher.class));
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

    private void onAttack(final Demon demon){
        db.collection("stashes").document(currentStash.getId().toString()).collection("demons").get().addOnCompleteListener(
            new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    ArrayList<Demon> defenders = new ArrayList<>();
                    if (task.isSuccessful()){
                        for (QueryDocumentSnapshot document : task.getResult()){
                            Demon defender = new Demon(document.getData());
                            defenders.add(defender);
                            Log.i(TAG, "defending demon " + defender.toString());
                        }
                        battle.attackStash(demon,defenders,currentStash);
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
        StashUtils.updateRadius(db,currentStash);
        db.collection("stashes").document(nullStashId.toString()).collection("demons").document(demon.getId().toString()).delete();
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
