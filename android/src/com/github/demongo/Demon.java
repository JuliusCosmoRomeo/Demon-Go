package com.github.demongo;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Demon implements Parcelable {

    private final String NAME = "name";
    private final String HP = "hp";
    private final String MAXHP = "maxhp";
    private final String WORTH = "worth";
    private final String ATTACK_PTS = "attack_pts";
    private final String RESOURCE = "resource";
    private final String TYPE = "type";
    private final String ID = "id";
    private final String STASH_ID = "stash_id";


    public enum Type {
        Imp,
        Foliot,
        Djinn,
        Afrit,
        Marid
    }

    private String name;
    private long hp;
    private final long maxHp;
    private final long worth;
    private final long attackPoints;
    private final long resource;
    private final Type type;
    private final ParcelUuid id;
    private final ParcelUuid stashId;

    private Map<String,Object> map;


    protected Demon(Parcel in) {

        //Log.i("demon-go-demon", in.marshall().toString());
        name = in.readString();
        hp = in.readLong();
        maxHp = in.readLong();
        worth = in.readLong();
        attackPoints = in.readLong();
        resource = in.readLong();
        type = Type.valueOf(in.readString());
        stashId = in.readParcelable(ParcelUuid.class.getClassLoader());
        id = in.readParcelable(ParcelUuid.class.getClassLoader());
        initMap();
    }

    public static final Creator<Demon> CREATOR = new Creator<Demon>() {
        @Override
        public Demon createFromParcel(Parcel in) {
            return new Demon(in);
        }

        @Override
        public Demon[] newArray(int size) {
            return new Demon[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeLong(hp);
        dest.writeLong(maxHp);
        dest.writeLong(worth);
        dest.writeLong(attackPoints);
        dest.writeLong(resource);
        dest.writeString(type.toString());
        dest.writeParcelable(stashId,flags);
        dest.writeParcelable(id,flags);
    }

    public Demon(String name, int hp, int maxHp, int attackPoints, int worth, int resource, Type type, ParcelUuid stashId, ParcelUuid id){
        this.name = name;
        this.maxHp = maxHp;
        this.hp = hp;
        this.attackPoints = attackPoints;
        this.worth = worth;
        this.resource = resource;
        this.type = type;
        this.stashId = stashId;
        this.id = id;
        initMap();
    }

    private void initMap(){
        this.map = new HashMap<String, Object>();
        this.map.put(ID,id.toString());
        this.map.put(STASH_ID,stashId.toString());
        this.map.put(NAME,name);
        this.map.put(MAXHP,maxHp);
        this.map.put(HP,hp);
        this.map.put(WORTH,worth);
        this.map.put(ATTACK_PTS,attackPoints);
        this.map.put(RESOURCE,resource);
        this.map.put(TYPE,type.toString());
    }

    public Demon(Map<String, Object> map){
        this.map = map;
        this.id = map.get(ID) != null ? ParcelUuid.fromString((String) map.get(ID)) : new ParcelUuid(UUID.randomUUID());
        this.stashId = map.get(STASH_ID) != null ? ParcelUuid.fromString((String) map.get(STASH_ID)) : new ParcelUuid(UUID.randomUUID());
        this.maxHp = map.get(MAXHP) != null ? (long) map.get(MAXHP) : -1;
        this.hp = map.get(HP) != null ? (long) map.get(HP) : -1;
        this.worth = map.get(WORTH) != null ? (long) map.get(WORTH) : -1;
        this.attackPoints = map.get(ATTACK_PTS) != null ? (long) map.get(ATTACK_PTS) : -1;
        this.resource = map.get(RESOURCE) != null ? (long) map.get(RESOURCE) : -1;
        this.type = map.get(TYPE) != null ? Type.valueOf((String) map.get(TYPE)) : Type.Imp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getHp() {
        return hp;
    }

    public void setHp(long hp) {
        this.hp = hp;
    }

    public long getMaxHp() {
        return maxHp;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public long getAttackPoints() {
        return attackPoints;
    }

    public long getResource() {
        return resource;
    }

    public Type getType() {
        return type;
    }

    public long getWorth() {
        return worth;
    }

    public String toString(){
        return name + " " + hp + "/" + maxHp + " worth " + worth + " attack " + attackPoints + " " + type +
                " Demon id " + getId() + " Stash id " + getStashId();
    }

    public ParcelUuid getId() {
        return id;
    }

    public ParcelUuid getStashId() {
        return stashId;
    }

}