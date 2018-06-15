package com.github.demongo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

class LocationQuery {
    private static final int REQUEST_CODE = 7;
    private static final float MIN_ACCURACY_METERS = 100;
    private static final int WAIT_MILLIS = 10 * 1000;

    public interface LocationFoundListener {
        void onFoundLocation(Location location);
        void onError(String msg);
    }

    private Location currentBestLocation;
    private boolean waiting = true;
    private boolean emittedBest = false;
    private LocationFoundListener listener;

    LocationQuery(Context context, LocationFoundListener _listener) {
        listener = _listener;

        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)  {
            listener.onError("No Location Service available");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE);
            listener.onError("Please grant permission and restart app");
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!waiting)
                    return;

                waiting = false;
                if (currentBestLocation != null) {
                    listener.onFoundLocation(currentBestLocation);
                    emittedBest = true;
                }
            }
        }, WAIT_MILLIS);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("demon-go-pvp", "Found location " + location.getAccuracy() + " " + MIN_ACCURACY_METERS);
                if (isBetterLocation(location, currentBestLocation)) {
                    currentBestLocation = location;

                    if (location.getAccuracy() < MIN_ACCURACY_METERS) {
                        waiting = false;
                        emittedBest = true;
                        listener.onFoundLocation(location);
                    }
                }

                if (!waiting) {
                    locationManager.removeUpdates(this);
                    if (!emittedBest) {
                        listener.onFoundLocation(currentBestLocation);
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.e("demon-go-pvp", provider);
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.e("demon-go-pvp", "enabled " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.e("demon-go-pvp", "disabled " + provider);
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * https://developer.android.com/guide/topics/location/strategies
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
