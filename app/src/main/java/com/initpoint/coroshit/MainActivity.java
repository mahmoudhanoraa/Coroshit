package com.initpoint.coroshit;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import net.glxn.qrgen.android.QRCode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private static String AUTH_TAG = "auth";
    private ImageView qrCodeTV;
    private static int PERMISSION_REQUEST_CODE = 123;
    private int RED = 0xFFFF0000;
    private int BLACK = 0xFF000000;
    private int YELLOW = 0xFFffbe4f;
    private FirebaseDatabase realtimeDatabase;
    private DatabaseReference rtDatabaseReference;
    private Switch service_btn;
    ProgressDialog progress;
    Boolean eventFlag = false;

    Button btn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        qrCodeTV = findViewById(R.id.qr_code);
        auth = FirebaseAuth.getInstance();
        realtimeDatabase = FirebaseDatabase.getInstance();
        rtDatabaseReference = realtimeDatabase.getReference();
        progress = new ProgressDialog(this);
        service_btn = findViewById(R.id.service_btn);


        service_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Intent intent = new Intent(MainActivity.this, LocationTrackerService.class);
                    startService(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, LocationTrackerService.class);
                    stopService(intent);
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
                    List<String> allKeys = Paper.book(date).getAllKeys();
                    Log.d("location", "locations saved " + allKeys.size());
                }
            }
        });


        Paper.init(this);


//        Button btn2 = findViewById(R.id.button3);
//        btn2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                changeStatus(auth.getCurrentUser());
//            }
//        });
//        btn2.setVisibility(View.GONE);

        btn3 = findViewById(R.id.button4);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sync();
            }
        });


        boolean permissionAccessCoarseLocationApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;


        if (permissionAccessCoarseLocationApproved) {
            boolean permissionAccessFineLocationApproved =
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            if (permissionAccessFineLocationApproved) {

                boolean permissionForegroundServicesApproved =
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                                == PackageManager.PERMISSION_GRANTED;
                if (permissionForegroundServicesApproved) {
                    //everything is good
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{
                                    Manifest.permission.FOREGROUND_SERVICE
                            },
                            PERMISSION_REQUEST_CODE);
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        PERMISSION_REQUEST_CODE);
            }

        } else {
            // App doesn't have access to the device's location at all. Make full request
            // for permission.
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            Toast.makeText(MainActivity.this, "User is logged in",
                    Toast.LENGTH_SHORT).show();
            updateUI(currentUser);
            rtDatabaseReference.child(auth.getCurrentUser().getUid()).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    if(!eventFlag){
                        Log.d("FLAG","Added");
                        int user_status = Paper.book("user_status").read("user_status", 0);
                        if(user_status != 2){
                            changeStatus(auth.getCurrentUser());
                        }
                        eventFlag = true;
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    Log.d("FLAG","Changed");

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("FLAG","Removed");

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    Log.d("FLAG","Moved");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            signInUseresAnonymously();
        }

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("registrationID", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
//                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d("registrationID", token);
                    }
                });

    }

    private void signInUseresAnonymously() {
        auth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(AUTH_TAG, "signInAnonymously:success");
                            FirebaseUser user = auth.getCurrentUser();
                            updateUI(user);
                            rtDatabaseReference.child(auth.getCurrentUser().getUid()).addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                    if(!eventFlag){
                                        Log.d("FLAG","Added");
                                        int user_status = Paper.book("user_status").read("user_status", 0);
                                        if(user_status != 2){
                                            changeStatus(auth.getCurrentUser());
                                        }
                                        eventFlag = true;
                                    }
                                }

                                @Override
                                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                    Log.d("FLAG","Changed");

                                }

                                @Override
                                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                                    Log.d("FLAG","Removed");

                                }

                                @Override
                                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                                    Log.d("FLAG","Moved");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(AUTH_TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        Bitmap qrCode;
        if (user != null) {
            int user_status = Paper.book("user_status").read("user_status", 0);
            if (user_status == 2) {
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(RED, 0xFFFFFFFF).bitmap();
            } else if (user_status == 0) {
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(BLACK, 0xFFFFFFFF).bitmap();
            } else {
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(YELLOW, 0xFFFFFFFF).bitmap();
            }

            qrCodeTV.setImageBitmap(qrCode);
        }
    }

    private void changeStatus(FirebaseUser user) {
        Bitmap qrCode;
        if (user != null) {
            qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(RED, 0xFFFFFFFF).bitmap();
            Paper.book("user_status").write("user_status", 2);
            //Bitmap qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(RED, 0xFFFFFFFF).bitmap();
            qrCodeTV.setImageBitmap(qrCode);
            List<String> allDays = Paper.book("days").read("availableDays", new ArrayList<String>());
            String autoGenKey = rtDatabaseReference.child("confirmed-cases").push().getKey();
            Map<String, Object> childUpdates = new HashMap<>();
            long unixTime = System.currentTimeMillis();
            Log.d("key", String.valueOf(unixTime));

            for (String dayStr : allDays) {
                List<String> allKeys = Paper.book(dayStr).getAllKeys();
                for (String key : allKeys) {
                    Map<String, Object> data = new HashMap<>();
                    Log.d("key", key);

                    if (key.contains("-lat")) {
                        String lat = Paper.book(dayStr).read(key, null);
                        childUpdates.put("confirmed-cases/" + autoGenKey + '/' + dayStr + '/' + key.split("-")[0] + '/' + "Lat", lat);
                        Log.d("firebase-lat", lat);
                    } else {
                        String lon = Paper.book(dayStr).read(key, null);
                        childUpdates.put("confirmed-cases/" + autoGenKey + '/' + dayStr + '/' + key.split("-")[0] + '/' + "Long", lon);
                        Log.d("firebase-long", lon);
                    }
                }
            }
            childUpdates.put("confirmed-cases/" + autoGenKey + "/timestamp", unixTime);
            rtDatabaseReference.updateChildren(childUpdates);
            btn3.setVisibility(View.GONE);
            service_btn.setVisibility(View.GONE);
        }
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

    private void sync() {
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        progress.show();
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
                                    Bitmap qrCode = QRCode.from(auth.getCurrentUser().getUid()).withSize(700, 700).withColor(YELLOW, 0xFFFFFFFF).bitmap();
                                    qrCodeTV.setImageBitmap(qrCode);
                                    Paper.book("user_status").write("user_status", 1);
                                    progress.dismiss();
                                    return;
                                }
                            }
                        }

                    }
                }
                progress.dismiss();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}

