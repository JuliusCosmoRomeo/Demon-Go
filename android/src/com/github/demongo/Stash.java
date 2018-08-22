package com.github.demongo;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import com.github.demongo.Map.MapActivity;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Stash implements Parcelable {
    private final String CAPACITY = "capacity";
    private final String FILLED = "filled";
    private final String POSITION = "position";
    private final String RADIUS = "radius";
    private final String PLAYER_ID = "player_id";
    private final String STASH_ID = "stash_id";

    private final ParcelUuid id;

    private ParcelUuid playerID;
    private final ParcelableGeoPoint location;
    private double radius;
    private final long capacity;

    private long filled;

    //in the map Geopoints are saved instead of ParcelableGeoPoints
    private Map<String,Object> map;

    public Stash(ParcelUuid id, ParcelUuid playerID, ParcelableGeoPoint location, double radius, long capacity, long filled) {
        this.id = id;
        this.playerID = playerID;
        this.location = location;
        this.radius = radius;
        this.capacity = capacity;
        this.filled = filled;
        this.map = new HashMap<String, Object>();
        this.map.put(PLAYER_ID,playerID.toString());
        this.map.put(STASH_ID,id.toString());
        this.map.put(POSITION,location.getGeoPoint()); //watch out -> in the map we save GeoPoints, not ParcelableGeoPoints
        this.map.put(RADIUS,radius);
        this.map.put(CAPACITY,capacity);
        this.map.put(FILLED,filled);
    }

    public Stash(Map<String, Object> map){
        this.map = map;
        this.id = map.get(STASH_ID) != null ? ParcelUuid.fromString((String) map.get(STASH_ID)) : new ParcelUuid(UUID.randomUUID());
        this.playerID = new ParcelUuid(MapActivity.playerId); // map.get(PLAYER_ID) != null ? ParcelUuid.fromString(map.get(PLAYER_ID) + "") :
        this.location = new ParcelableGeoPoint((GeoPoint) map.get(POSITION));
        this.capacity = map.get(CAPACITY) != null ? (long) map.get(CAPACITY) : -1;
        this.filled = map.get(FILLED) != null ? (long) map.get(FILLED) : -1;
        this.radius = map.get(RADIUS) != null ? (double) map.get(RADIUS) : -1;
    }

    protected Stash(Parcel in) {
        //does the order of values make a difference?
        capacity = in.readLong();
        radius = in.readDouble();
        playerID = in.readParcelable(ParcelUuid.class.getClassLoader());
        id = in.readParcelable(ParcelUuid.class.getClassLoader());
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

    public ParcelUuid getId() {
        return id;
    }

    public ParcelUuid getPlayerID() {
        return playerID;
    }

    public void setPlayerID(ParcelUuid playerID) {
        this.playerID = playerID;
    }
    public GeoPoint getLocation() {
        return location.getGeoPoint();
    }

    public double getRadius() {
        return radius;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getFilled() {
        return filled;
    }

    public void setRadius(double radius) {
        this.radius = radius;
        this.map.put(RADIUS,this.radius);
    }

    public void setFilled(long filled) {
        this.filled = filled;
        this.map.put(FILLED,this.filled);
    }

    public String toString(){
        return "Stash: player " + getPlayerID() + " position(" + this.getLocation().getLatitude() + "," + this.getLocation().getLongitude() + "), radius " +
                getRadius() + " capacity " + getFilled() + "/" + getCapacity() + " Stash id " + getId();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(radius);
        out.writeLong(capacity);
        out.writeLong(filled);
        out.writeParcelable(playerID,flags);
        out.writeParcelable(id,flags);
        out.writeParcelable(location,flags);
    }

}
