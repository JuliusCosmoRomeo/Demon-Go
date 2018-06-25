package com.github.demongo;

import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class Stash {
    public final int MAX_CAPACITY = 10000;
    public final int MAX_RADIUS = 500;
    private final String CAPACITY = "capacity";
    private final String FILLED = "filled";
    private final String POSITION = "position";
    private final String PLAYER_ID = "player_id";


    private final int playerID;
    private final GeoPoint location;
    private final int capacity;
    private final int filled;
    private Map<String,Object> map;

    public Stash (int playerID, GeoPoint location, int capacity, int filled) {

        this.playerID = playerID;
        this.location = location;
        this.capacity = capacity;
        this.filled = filled;
        this.map = new HashMap<String, Object>();
        this.map.put(PLAYER_ID,playerID);
        this.map.put(POSITION,location);
        this.map.put(CAPACITY,capacity);
        this.map.put(FILLED,filled);
    }

    public Stash(int filled, HashMap<String, Object> map){
        this.map = map;
        this.playerID = (int) map.get(PLAYER_ID);
        this.location = (GeoPoint) map.get(POSITION);
        this.capacity = (int) map.get(CAPACITY);
        this.filled = (int) map.get(FILLED);
    }

    public Map<String, Object> getMap() {
        return map;
    }


}
