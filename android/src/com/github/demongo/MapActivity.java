package com.github.demongo;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap map) {
                mapboxMap = map;
                //add the demon icon as resource to the map
                Bitmap icon = BitmapFactory.decodeResource(
                        MapActivity.this.getResources(), R.drawable.demon);
                mapboxMap.addImage(MARKER_IMAGE, icon);

                setupBuildings();
                fetchMarkers();
                mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(@NonNull LatLng point) {
                        Stash stash = new Stash(new ParcelUuid(UUID.randomUUID()), new ParcelUuid(playerId),
                                new ParcelableGeoPoint(new GeoPoint(point.getLatitude(), point.getLongitude())),
                                ThreadLocalRandom.current().nextInt(1, 5 + 1),
                                1000,
                                0);

                        //addNewMarker(stash);
                        addDemonMarker(new Demon("luschi",100,30,230, R.drawable.notification_icon,Demon.Type.Imp),point);
                    }
                });
                /*mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        StashMarker stashMarker = (StashMarker) marker;
                        Intent stashIntent = new Intent(MapActivity.this,StashActivity.class);
                        stashIntent.setExtrasClassLoader(ParcelableGeoPoint.class.getClassLoader());
                        stashIntent.putExtra("stash", stashMarker.getStash());
                        startActivity(stashIntent);
                        return true;
                    }
                });*/

                addCustomInfoWindowAdapter();
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
                if (isCurrentPlayer(stash.getPlayerID())){
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
                    buttonContainer.addView(attackBtn);
                }


                return container;
            }
        });
    }

    void fetchMarkers() {
        db.collection("stashes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null || snapshot == null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                for (QueryDocumentSnapshot doc : snapshot) {
                    Log.i(TAG, "stash id " + doc.getId());

                    Stash stash = new Stash(doc.getData());
                    if (!doc.getId().toString().contains("-")){

                        Log.i(TAG, " old stash " + stash.toString());
                        db.collection("stashes").document(doc.getId().toString())
                                .delete();
                    }
                    GeoPoint position = stash.getLocation();
                    if (position != null)  {
                        Log.i(TAG, "Radius " + stash.getRadius());
                        Log.i(TAG, "Filled " + stash.getFilled());

                        boolean isCurrentPlayer = isCurrentPlayer(stash.getPlayerID());
                        if (stash.getRadius()==-1){
                            stash.setRadius(1);
                        }
                        addMarker(stash, isCurrentPlayer);
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
            polygon.add(new LatLng(position.getLatitude() + y, position.getLongitude() + x));
        }

        return polygon;
    }

    private void addMarker(Stash stash, boolean isCurrentPlayer) {
        LatLng pos = new LatLng(stash.getLocation().getLatitude(), stash.getLocation().getLongitude());
        StashMarkerOptions marker = new StashMarkerOptions(stash);
        marker.setPosition(pos);
        mapboxMap.addMarker(marker);


        List<LatLng> polygon = getStashPerimeter(pos,stash.getRadius());
        String colorString = isCurrentPlayer ?  "#00ff3300" : "#33ff0000";
        mapboxMap.addPolygon(new PolygonOptions().addAll(polygon).fillColor(Color.parseColor(colorString)));
    }

    private void addNewMarker(Stash stash) {
        addMarker(stash, true);
        db.collection("stashes").document(stash.getId().toString()).set(stash.getMap());

    }

    private void addDemonMarker(Demon demon, LatLng target) {
        List<Feature> features = new ArrayList<>();
        /* Source: A data source specifies the geographic coordinate where the image marker gets placed. */
        features.add(Feature.fromGeometry(Point.fromLngLat(target.getLongitude(),target.getLatitude())));
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

    private boolean isCurrentPlayer(ParcelUuid uuid){
        if (uuid==null){
            return false;
        }
        return uuid.getUuid() == this.playerId;
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
                        Log.i(TAG, "deposited " + slider.getProgress() + " moneydos");
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

// 3. Get the AlertDialog from create()
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GALLERY_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Demon demon = data.getParcelableExtra("demon");
                Log.i(TAG,demon.toString());
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                addDemonMarker(demon,new LatLng(currentStash.getLocation().getLatitude(), currentStash.getLocation().getLongitude()));
                // Do something with the contact here (bigger example below)
            }
        }
    }

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

    class StashMarkerOptions extends BaseMarkerOptions<StashMarker, StashMarkerOptions> {

        private Stash stash;

        public StashMarkerOptions stash(Stash stash) {
            this.stash = stash;
            return getThis();
        }

        public StashMarkerOptions(Stash stash) {
            super();
            this.stash = stash;
        }

        private StashMarkerOptions(Parcel in) {
            position((LatLng) in.readParcelable(LatLng.class.getClassLoader()));
            snippet(in.readString());
            String iconId = in.readString();
            Bitmap iconBitmap = in.readParcelable(Bitmap.class.getClassLoader());
            Icon icon = IconFactory.recreate(iconId, iconBitmap);
            icon(icon);
            stash((Stash)in.readParcelable(Stash.class.getClassLoader()));
        }

        @Override
        public StashMarkerOptions getThis() {
            return this;
        }

        @Override
        public StashMarker getMarker() {
            return new StashMarker(this, stash);
        }

        public final Parcelable.Creator<StashMarkerOptions> CREATOR
                = new Parcelable.Creator<StashMarkerOptions>() {
            public StashMarkerOptions createFromParcel(Parcel in) {
                return new StashMarkerOptions(in);
            }

            public StashMarkerOptions[] newArray(int size) {
                return new StashMarkerOptions[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(position, flags);
            out.writeString(snippet);
            out.writeString(icon.getId());
            out.writeParcelable(icon.getBitmap(), flags);
            out.writeParcelable(stash,flags);
        }
    }
}
