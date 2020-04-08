package com.initpoint.coroshit;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.LocationCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.paperdb.Paper;

public class SyncService extends Service {
    MainActivity mainActivity = new MainActivity();
    public static final String CHANNEL_ID2 = "NotificationChannel";
    private LocationCallback locationCallback;
    NotificationManager mNotifyManager ;
    NotificationCompat.Builder mBuilder ;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private boolean possibleInfection(LocationToSave loc1, LocationToSave loc2) {
        return LocationToSave.distanceBetween(loc1, loc2) < 1000 && Math.abs(loc1.getTimestamp() - loc2.getTimestamp()) < 14400000;
    }

    private ArrayList<LocationToSave> getLocations(String dayStr) {
        List<String> allKeys = Paper.book(dayStr).getAllKeys();
        Map<String, Boolean> added = new HashMap<>();
        ArrayList<LocationToSave> locations = new ArrayList<>();
        for (String key : allKeys) {
            String timestamp = key.split("-")[0];
            if (!added.containsKey(timestamp)) {
                added.put(key, true);
                String lat = Paper.book(dayStr).read(timestamp + "-lat", null);
                String lon = Paper.book(dayStr).read(timestamp + "-long", null);
                locations.add(new LocationToSave(Double.valueOf(lat), Double.valueOf(lon), Long.valueOf(timestamp)));
            }
        }
        return locations;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final ResultReceiver result_reciever = intent.getParcelableExtra("reciever");
        final Bundle bundle = new Bundle();
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this,CHANNEL_ID2);
        mBuilder.setContentTitle("CoroShit")
                .setContentText("Syncing")
                .setSmallIcon(R.drawable.ic_sync);




        mBuilder.setProgress(0, 0, true);
        mNotifyManager.notify(0, mBuilder.build());
        long lastSyncTimestamp = Paper.book().read("last-updated-timestamp", 0L);
        Log.d("TEST", String.valueOf(lastSyncTimestamp));
        mainActivity.getRtDatabaseReference().child("confirmed-cases").orderByChild("timestamp").startAt(lastSyncTimestamp + 1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Toast.makeText(getApplicationContext(),String.valueOf(dataSnapshot.getChildrenCount()),Toast.LENGTH_SHORT).show();
                List<String> allDays = Paper.book("days").read("availableDays", new ArrayList<String>());
                for (DataSnapshot caseSnapshot : dataSnapshot.getChildren()) {
                    for (String dayStr : allDays) {
                        ArrayList<LocationToSave> locations = getLocations(dayStr);
                        Long patient_timestamp = (Long) caseSnapshot.child("timestamp").getValue();
                        Log.d("TEST", String.valueOf(patient_timestamp));
                        Paper.book().write("last-updated-timestamp", patient_timestamp);
                        for (DataSnapshot location : caseSnapshot.child(dayStr).getChildren()) {
                            for (LocationToSave loc : locations) {
                                Double lat = Double.valueOf(location.child("Lat").getValue().toString());
                                Double lon = Double.valueOf(location.child("Long").getValue().toString());
                                Long timestamp = Long.valueOf(location.getKey());
                                Log.d("test", String.valueOf(possibleInfection(loc, new LocationToSave(lat, lon, timestamp))));
                                if (possibleInfection(loc, new LocationToSave(lat, lon, timestamp))) {


                                    Paper.book("user_status").write("user_status","may_be_infected");
                                    FirebaseUser currentUser = mainActivity.getAuth().getCurrentUser();

                                    mBuilder.setContentTitle("sync Completed").setContentText("You may be infected harry up and make Corona Test ")
                                            .setProgress(0,0,false);
                                    mNotifyManager.notify(0, mBuilder.build());
                                    result_reciever.send(1,bundle);
                                    stopSelf();
                                    return;
                                }


                            }
                        }

                    }
                }

                mBuilder.setContentTitle("sync Completed").setContentText("You are safe ")
                        .setProgress(0,0,false);
                mNotifyManager.notify(0, mBuilder.build());
                result_reciever.send(1,bundle);
                stopSelf();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
