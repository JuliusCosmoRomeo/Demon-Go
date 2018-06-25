package com.github.demongo;

import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class Stash {
    public final long MAX_CAPACITY = 10000;
    public final long MAX_RADIUS = 500;
    private final String CAPACITY = "capacity";
    private final String FILLED = "filled";
    private final String POSITION = "position";
    private final String RADIUS = "radius";
    private final String PLAYER_ID = "player_id";


    private final long playerID;
    private final GeoPoint location;
    private final long radius;
    private final long capacity;

    private final long filled;

    private Map<String,Object> map;

    public Stash(long playerID, GeoPoint location, long radius, long capacity, long filled) {

        this.playerID = playerID;
        this.location = location;
        this.radius = radius;
        this.capacity = capacity;
        this.filled = filled;
        this.map = new HashMap<String, Object>();
        this.map.put(PLAYER_ID,playerID);
        this.map.put(POSITION,location);
        this.map.put(RADIUS,radius);
        this.map.put(CAPACITY,capacity);
        this.map.put(FILLED,filled);
    }

    public Stash(Map<String, Object> map){
        this.map = map;
        this.playerID = map.get(PLAYER_ID) != null ? (long) map.get(PLAYER_ID) : -1;
        this.location = (GeoPoint) map.get(POSITION);
        this.capacity = map.get(CAPACITY) != null ? (long) map.get(CAPACITY) : -1;
        this.filled = map.get(FILLED) != null ? (long) map.get(FILLED) : -1;
        this.radius = map.get(RADIUS) != null ? (long) map.get(RADIUS) : -1;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public long getPlayerID() {
        return playerID;
    }

    public GeoPoint getLocation() {
        return location;
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
}
