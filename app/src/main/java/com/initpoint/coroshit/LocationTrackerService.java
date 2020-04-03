package com.initpoint.coroshit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.arch.core.util.Function;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;

public class LocationTrackerService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private LocationCallback locationCallback;


    public FusedLocationProviderClient fusedLocationClient;

    public LocationTrackerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "Invoke background service onCreate method.", Toast.LENGTH_LONG).show();
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location tracker")
                .setContentText("We are tracking your location")
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        final Location[] lastLoc = new Location[1];
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (final Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    if (lastLoc[0] == null)
                        lastLoc[0] = location;
//                    float [] results = new float[3];
//                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), lastLoc[0].getLatitude(), lastLoc[0].getLongitude(),results );
                    final String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    new Thread(new Runnable() {
                        public void run() {
                            Paper.book(date).write(location.getTime() + "-lat", String.valueOf(location.getLatitude()));
                            Paper.book(date).write(location.getTime() + "-long", String.valueOf(location.getLongitude()));
                        }
                    }).start();

                    new Thread(new Runnable() {
                        public void run() {
                            List<String> allDays = Paper.book("days").read("availableDays", new ArrayList<String>());
                            if (!allDays.contains(date)) {
                                if (allDays.size() < 15) {
                                    allDays.add(date);
                                } else {
                                    List<Date> allDates = new ArrayList<>();
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd",  Locale.getDefault());
                                    for (String day : allDays) {
                                        try {
                                             allDates.add(formatter.parse(day));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    Date oldDate = Collections.min(allDates);
                                    String oldDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(oldDate);
                                    allDays.remove(oldDateStr);
                                    Paper.book(oldDateStr).destroy();
                                    allDays.add(date);
                                }
                                Paper.book("days").write("availableDays", allDays);
                            }
                        }
                    }).start();
                }
            }
        };
        startLocationUpdates();
        Toast.makeText(this, "Invoke background service onStartCommand method.", Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Invoke background service onDestroy method.", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

}
