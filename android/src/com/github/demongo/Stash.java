package com.github.demongo;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Stash implements Parcelable {
    public final long MAX_CAPACITY = 10000;
    public final long MAX_RADIUS = 500;
    private final String CAPACITY = "capacity";
    private final String FILLED = "filled";
    private final String POSITION = "position";
    private final String RADIUS = "radius";
    private final String PLAYER_ID = "player_id";


    private final long playerID;
    private final ParcelableGeoPoint location;
    private long radius;
    private final long capacity;
    private final long filled;

    //in the map Geopoints are saved instead of ParcelableGeoPoints
    private Map<String,Object> map;

    public Stash(long playerID, ParcelableGeoPoint location, long radius, long capacity, long filled) {

        this.playerID = playerID;
        this.location = location;
        this.radius = radius;
        this.capacity = capacity;
        this.filled = filled;
        this.map = new HashMap<String, Object>();
        this.map.put(PLAYER_ID,playerID);
        this.map.put(POSITION,location.getGeoPoint()); //watch out -> in the map we save GeoPoints, not ParcelableGeoPoints
        this.map.put(RADIUS,radius);
        this.map.put(CAPACITY,capacity);
        this.map.put(FILLED,filled);
    }

    public Stash(Map<String, Object> map){
        this.map = map;
        this.playerID = map.get(PLAYER_ID) != null ? (long) map.get(PLAYER_ID) : -1;
        this.location = new ParcelableGeoPoint((GeoPoint) map.get(POSITION));
        this.capacity = map.get(CAPACITY) != null ? (long) map.get(CAPACITY) : -1;
        this.filled = map.get(FILLED) != null ? (long) map.get(FILLED) : -1;
        this.radius = map.get(RADIUS) != null ? (long) map.get(RADIUS) : -1;
    }

    protected Stash(Parcel in) {
        capacity = in.readLong();
        radius = in.readLong();
        playerID = in.readLong();
        filled = in.readLong();
        location = in.readParcelable(ParcelableGeoPoint.class.getClassLoader());
    }

    public static final Creator<Stash> CREATOR = new Creator<Stash>() {
        @Override
        public Stash createFromParcel(Parcel in) {
            return new Stash(in);
        }

        @Override
        public Stash[] newArray(int size) {
            return new Stash[size];
        }
    };

    public Map<String, Object> getMap() {
        return map;
    }

    public long getPlayerID() {
        return playerID;
    }

    public GeoPoint getLocation() {
        return location.getGeoPoint();
    }

    public long getRadius() {
        return radius;
    }

    public long getCapacity() {
        return capacity;
    }


    public long getFilled() {
        return filled;
    }

    public void setRadius(long radius) {
        this.radius = radius;
    }

    public String toString(){
        return "Stash: player " + getPlayerID() + " position(" + this.getLocation().getLatitude() + "," + this.getLocation().getLongitude() + "), radius " +
                getRadius() + " capacity " + getFilled() + "/" + getCapacity();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(radius);
        out.writeLong(capacity);
        out.writeLong(filled);
        out.writeLong(playerID);
        out.writeParcelable(location,flags);
    }
}
