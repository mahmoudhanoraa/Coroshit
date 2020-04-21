package com.initpoint.coroshit;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    public Fcmhandler() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // put your code here ya 7anora
    }

//    private void sync() {
//
//
//        realtimeDatabase = FirebaseDatabase.getInstance();
//        rtDatabaseReference = realtimeDatabase.getReference();
//
//        long lastSyncTimestamp = Paper.book().read("last-updated-timestamp", 0L);
//        Log.d("TEST", String.valueOf(lastSyncTimestamp));
//        rtDatabaseReference.child("confirmed-cases").orderByChild("timestamp").startAt(lastSyncTimestamp + 1).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                List<String> allDays = Paper.book("days").read("availableDays", new ArrayList<String>());
//                for (DataSnapshot caseSnapshot : dataSnapshot.getChildren()) {
//                    for (String dayStr : allDays) {
//                        ArrayList<LocationToSave> locations = getLocations(dayStr);
//                        Long patient_timestamp = (Long) caseSnapshot.child("timestamp").getValue();
//                        Log.d("TEST", String.valueOf(patient_timestamp));
//                        Paper.book().write("last-updated-timestamp", patient_timestamp);
//                        for (DataSnapshot location : caseSnapshot.child(dayStr).getChildren()) {
//                            for (LocationToSave loc : locations) {
//                                Double lat = Double.valueOf(location.child("Lat").getValue().toString());
//                                Double lon = Double.valueOf(location.child("Long").getValue().toString());
//                                Long timestamp = Long.valueOf(location.getKey());
//                                Log.d("test", String.valueOf(possibleInfection(loc, new LocationToSave(lat, lon, timestamp))));
//                                if (possibleInfection(loc, new LocationToSave(lat, lon, timestamp))) {
//                                    Bitmap qrCode = QRCode.from(auth.getCurrentUser().getUid()).withSize(700, 700).withColor(YELLOW, 0xFFFFFFFF).bitmap();
//                                    qrCodeTV.setImageBitmap(qrCode);
//                                    Paper.book("user_status").write("user_status", 1);
//                                    progress.dismiss();
//                                    return;
//                                }
//                            }
//                        }
//
//                    }
//                }
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//
//    }
//    private ArrayList<LocationToSave> getLocations(String dayStr) {
//        List<String> allKeys = Paper.book(dayStr).getAllKeys();
//        Map<String, Boolean> added = new HashMap<>();
//        ArrayList<LocationToSave> locations = new ArrayList<>();
//        for (String key : allKeys) {
//            String timestamp = key.split("-")[0];
//            if (!added.containsKey(timestamp)) {
//                added.put(key, true);
//                String lat = Paper.book(dayStr).read(timestamp + "-lat", null);
//                String lon = Paper.book(dayStr).read(timestamp + "-long", null);
//                locations.add(new LocationToSave(Double.valueOf(lat), Double.valueOf(lon), Long.valueOf(timestamp)));
//            }
//        }
//        return locations;
//    }
}

