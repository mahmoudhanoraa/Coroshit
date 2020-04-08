package com.initpoint.coroshit;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private Button btn3 ;
    private static int PERMISSION_REQUEST_CODE = 123;
    private int RED = 0xFFFF0000;
    private int BLACK = 0xFF000000;
    private int YELLOW = 0xFFffbe4f;
    private FirebaseDatabase realtimeDatabase;
    private DatabaseReference rtDatabaseReference;
    private Switch service_btn ;
    private Boolean infected ;
    ProgressDialog progress;
    Handler handler = new Handler();

    public FirebaseAuth getAuth() {
        auth = FirebaseAuth.getInstance();
        return auth;
    }

    public DatabaseReference getRtDatabaseReference() {
        realtimeDatabase = FirebaseDatabase.getInstance();
        rtDatabaseReference = realtimeDatabase.getReference();
        return rtDatabaseReference;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Paper.init(this);
        qrCodeTV = findViewById(R.id.qr_code);
        auth = FirebaseAuth.getInstance();
        realtimeDatabase = FirebaseDatabase.getInstance();
        rtDatabaseReference = realtimeDatabase.getReference();
        progress = new ProgressDialog(this);
        infected = false ;
        service_btn = (Switch) findViewById(R.id.service_btn);
        service_btn.setChecked(Paper.book().read("sercive_btn_state",false));


        service_btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    Intent intent = new Intent(MainActivity.this, LocationTrackerService.class);
                    startService(intent);
                    Paper.book().write("sercive_btn_state",true);
                }
                else {
                    Intent intent = new Intent(MainActivity.this, LocationTrackerService.class);
                    stopService(intent);
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
                    List<String> allKeys = Paper.book(date).getAllKeys();
                    Log.d("location", "locations saved " + allKeys.size());
                    Paper.book().write("sercive_btn_state",false);
                }
            }
        });







        Button btn2 = findViewById(R.id.button3);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infected = !infected ;
                changeStatus(auth.getCurrentUser());
            }
        });
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
        } else {
            signInUseresAnonymously();
        }
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

    public void updateUI(FirebaseUser user) {
        Bitmap qrCode ;
        if (user != null) {
            String user_status = Paper.book("user_status").read("user_status","not_infected");
            if(user_status.equals("infected")){
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(RED, 0xFFFFFFFF).bitmap();
            }
            else if(user_status.equals("not_infected")) {
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(BLACK, 0xFFFFFFFF).bitmap();
            }

            else {
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(YELLOW, 0xFFFFFFFF).bitmap();
                Paper.book("user_status").write("user_status","not_infected");


            }

            qrCodeTV.setImageBitmap(qrCode);
        }
    }

    private void changeStatus(FirebaseUser user) {
        Bitmap qrCode ;
        if (user != null) {
            if(infected){
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(RED, 0xFFFFFFFF).bitmap();
                Paper.book("user_status").write("user_status","infected");
            }
            else {
                qrCode = QRCode.from(user.getUid()).withSize(700, 700).withColor(BLACK, 0xFFFFFFFF).bitmap();
                Paper.book("user_status").write("user_status","not_infected");
            }

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
          btn3.setEnabled(false);
          Intent sync_service = new Intent(MainActivity.this,SyncService.class);
          ResultReceiver result_receiver = new MyReciever(null);
          sync_service.putExtra("reciever",result_receiver);
          startService(sync_service);
//
        }
        public class MyReciever extends ResultReceiver{

            public MyReciever(Handler handler) {
                super(handler);
            }

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        btn3.setEnabled(true);
                        Log.i("Handlerrr","Handler works");
                        FirebaseUser currentUser = auth.getCurrentUser();
                        updateUI(currentUser);
                    }
                });
                super.onReceiveResult(resultCode, resultData);

            }
        }

    }



