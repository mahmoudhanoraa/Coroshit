package com.initpoint.coroshit;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.glxn.qrgen.android.QRCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.paperdb.Paper;

public class Fcmhandler extends  FirebaseMessagingService {
    private DatabaseReference rtDatabaseReference;
    private FirebaseDatabase realtimeDatabase;
    private FirebaseAuth auth;
    String registration_id="";
    public Fcmhandler() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("test","asdasd");
        if (remoteMessage.getData().size()>0)
        sync();
        // put your code here ya 7anora

    }


    private void sync() {


        realtimeDatabase = FirebaseDatabase.getInstance();
        rtDatabaseReference = realtimeDatabase.getReference();

        long lastSyncTimestamp = Paper.book().read("last-updated-timestamp", 0L);
        Log.d("TEST", String.valueOf(lastSyncTimestamp));
        rtDatabaseReference.child("confirmed-cases").orderByChild("timestamp").startAt(lastSyncTimestamp + 1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
                                    Paper.book("user_status").write("user_status", 1);
                                        getNotificationId();
                                    return;
                                }
                            }
                        }

                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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
    private boolean possibleInfection(LocationToSave loc1, LocationToSave loc2) {
        return LocationToSave.distanceBetween(loc1, loc2) < 1000 && Math.abs(loc1.getTimestamp() - loc2.getTimestamp()) < 14400000;
    }
    private void  getNotificationId(){
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("registrationID", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        registration_id = task.getResult().getToken();
                        DatabaseReference myRef = realtimeDatabase.getReference(auth.getCurrentUser().getUid()).child("registration_id");
                        myRef.setValue(registration_id);
                        // Log and toast
//                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d("registrationID", registration_id);

                    }
                });
    }
}


