package com.initpoint.coroshit;

import android.location.Location;
import android.util.Log;

public class LocationToSave {
    private Double lat;
    private Double lon;
    private long timestamp;

    public LocationToSave() {
    }

    public LocationToSave(Double lat, Double lon, long timestamp) {
        this.lat = lat;
        this.lon = lon;
        this.timestamp = timestamp;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public static float distanceBetween(LocationToSave loc1, LocationToSave loc2){
        float[] results = new float[3];
        Location.distanceBetween(loc1.lat, loc1.lon, loc2.lat, loc2.lon, results);
        Log.d("check", String.valueOf(results[0]));
        return results[0];
    }
}
