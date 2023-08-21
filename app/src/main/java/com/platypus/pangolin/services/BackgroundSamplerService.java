package com.platypus.pangolin.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.platypus.pangolin.R;
import com.platypus.pangolin.activities.MapActivity;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.samplers.AcousticNoiseSampler;
import com.platypus.pangolin.samplers.Sampler;
import com.platypus.pangolin.samplers.SignalStrengthSampler;
import com.platypus.pangolin.samplers.SimpleAcousticNoiseSampler;
import com.platypus.pangolin.samplers.WifiSampler;
import com.platypus.pangolin.utils.MGRSTools;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.tile.MGRSTileProvider;

public class BackgroundSamplerService extends Service {
    private DatabaseHelper db;
    private AcousticNoiseSampler noiseSampler;
    private SignalStrengthSampler signalSampler;
    private WifiSampler wifiSampler;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private MGRSTileProvider tp;

    int notification_id_counter = 0;

    public BackgroundSamplerService() {
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat
                .Builder(this, getString(R.string.notification_channel_id_background_service))
                .setContentTitle("Pangolin")
                .setContentText("Pangolin is sampling in background...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        db = new DatabaseHelper(this);
        noiseSampler = new AcousticNoiseSampler(500);
        wifiSampler = new WifiSampler(wifiManager);
        signalSampler = new SignalStrengthSampler(telephonyManager);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tp = MGRSTileProvider.create(this);

        String backgroundAccuracyKey = getString(R.string.background_accuracy_key);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String samplingValue = defaultSharedPreferences.getString(backgroundAccuracyKey, "3");
        if (samplingValue.equals("100 meters")) {
            SharedPreferences.Editor editor = defaultSharedPreferences.edit();
            editor.putString(backgroundAccuracyKey, "100 meters");
            editor.apply();
            samplingValue = "3";
        }
        int backgroundSamplingAccuracy = Integer.parseInt(samplingValue);
        System.out.println("Sampling accuracy " + samplingValue);

        int distanceInMeters;

        if (backgroundSamplingAccuracy== 3)
            distanceInMeters = 60;
        else if (backgroundSamplingAccuracy == 4)
            distanceInMeters = 600;
        else
            distanceInMeters = 5;

        locationRequest = new LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateDistanceMeters(distanceInMeters)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location lastLocation = locationResult.getLastLocation();

                if (lastLocation == null)
                    return;

                LatLng currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                MGRS currentLocationMGRS = tp.getMGRS(currentLocation);
                //System.out.println(currentLocationMGRS);

                String zone = currentLocationMGRS.getZone() + "" + currentLocationMGRS.getBand();
                String square = currentLocationMGRS.getColumnRowId();
                String easting = MGRSTools.getMaxAccuracyEN(currentLocationMGRS.getEasting());
                String northing = MGRSTools.getMaxAccuracyEN(currentLocationMGRS.getNorthing());

                Cursor dbQuery = db.getMissingSampleTypes(backgroundSamplingAccuracy, zone, square, easting, northing);
                System.out.println("Position registered");
                while(dbQuery.moveToNext()){
                    String res = dbQuery.getString(0);
                    System.out.println("Need to sample: " + res);
                    Sample newSample = smartSampler(res);

                    if (newSample == null){
                        System.out.println("Error while sampling " + res);
                        continue;
                    }

                    db.addSample(
                            newSample.getType().toString(),
                            newSample.getTimeStamp(),
                            newSample.getValue(),
                            newSample.getCondition(),
                            zone,
                            square,
                            easting,
                            northing
                    );

                    System.out.println("Correctly sampled " + res);

                    boolean notificationSettings = defaultSharedPreferences.getBoolean(getString(R.string.settings_notification_key), false);

                    if (notificationSettings) {
                        String notificationMsg = "Just sampled " + res + " with a value of " + newSample.getValue();
                        sendNotification(notificationMsg);
                    }
                }
            }
        };

        System.out.println("Service started");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        return START_REDELIVER_INTENT;
    }

    private Sample smartSampler(String sampleNeeded){
        Sampler sampler;

        if (sampleNeeded.equals("Noise"))
            sampler = new AcousticNoiseSampler(500);
        else if (sampleNeeded.equals("Wifi"))
            sampler = wifiSampler;
        else
            sampler = signalSampler;
        System.out.println("Sampling " + sampleNeeded);
        return sampler.getSample();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop location callback
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(String message) {
        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat
                .Builder(this, "basic")
                .setContentTitle("Pangolin had just sampled!")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notification_id_counter++, notification);

        // notificationId is a unique int for each notification that you must define
    }
}