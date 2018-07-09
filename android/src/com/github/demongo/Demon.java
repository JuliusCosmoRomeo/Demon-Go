package com.github.demongo;

import android.os.Parcel;
import android.os.Parcelable;

public class Demon implements Parcelable {


    public enum Type {
        Imp,
        Foliot,
        Djinn,
        Afrit,
        Marid
    }

    private String name;
    private int hp;
    private final int maxHp;
    private final int value;
    private final int attackPoints;
    private final int resource;
    private final Type type;


    protected Demon(Parcel in) {
        name = in.readString();
        hp = in.readInt();
        maxHp = in.readInt();
        value = in.readInt();
        attackPoints = in.readInt();
        resource = in.readInt();
        type = (Type) in.readSerializable();
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
        dest.writeInt(hp);
        dest.writeInt(maxHp);
        dest.writeInt(value);
        dest.writeInt(attackPoints);
        dest.writeInt(resource);
        dest.writeSerializable(type);



    }

    public Demon(String name, int maxHp, int attackPoints, int value, int resource, Type type){
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.attackPoints = attackPoints;
        this.value = value;
        this.resource = resource;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttackPoints() {
        return attackPoints;
    }

    public int getResource() {
        return resource;
    }

    public Type getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public String toString(){
        return name + " " + hp + "/" + maxHp + " value " + value + " attack " + attackPoints + " " +type;
    }
}