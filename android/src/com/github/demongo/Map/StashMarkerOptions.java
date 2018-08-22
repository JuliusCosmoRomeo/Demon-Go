package com.github.demongo.Map;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.github.demongo.Stash;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;

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

    public final Creator<StashMarkerOptions> CREATOR
            = new Creator<StashMarkerOptions>() {
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
