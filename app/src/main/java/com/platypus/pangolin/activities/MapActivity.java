package com.platypus.pangolin.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.platypus.pangolin.R;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.databinding.ActivityMapsBinding;
import com.platypus.pangolin.models.LocalizedSample;
import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.samplers.AcousticNoiseSampler;
import com.platypus.pangolin.samplers.Sampler;
import com.platypus.pangolin.samplers.SignalStrengthSampler;
import com.platypus.pangolin.samplers.WifiSampler;
import com.platypus.pangolin.services.BackgroundSamplerService;
import com.platypus.pangolin.utils.MGRSTools;
import com.platypus.pangolin.utils.MapManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;


public class MapActivity extends DrawerBaseActivity implements OnMapReadyCallback {
    //AIzaSyAwhBgttUCkbyHTYGSL49hZFSmESdli4cM api key
    private static final int REQUEST_MICROPHONE = 1;
    private static final int REQUEST_LOCATION = 2;
    private static final int REQUEST_ENABLE_GPS = 3;
    private static String SERVICE_CHANNEL_ID, BASIC_CHANNEL_ID;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton btn_sample, btn_resetDB, btn_synchData;
    private Button btn_noise, btn_signal, btn_wifi;
    private Button btn_10m, btn_100m, btn_1000m;
    private Button btn_localSamples, btn_worldSamples;
    private SampleType currentSampleType;
    private Sampler noiseSampler, signalSampler, wifiSampler, currentSampler;
    private GridType mapGridType;
    private DatabaseHelper db;
    private MapManager mapManager;
    private SharedPreferences defaultSharedPreferences;
    private String defaultSamplerKey;
    private String defaultGranularityKey;
    private String defaultSampler;
    private String defaultGranularity;
    private boolean backgroundSamplingOn;
    private FirebaseFirestore firebaseDB;
    private CollectionReference collectionReference;
    private boolean micPermissions, locationPermissions, GPSEnabled;
    private List<LocalizedSample> localizedSampleList;
    private String currentMap = "L";

    private void initializeSamplers() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        this.noiseSampler = new AcousticNoiseSampler(500);
        this.signalSampler = new SignalStrengthSampler(telephonyManager);
        this.wifiSampler = new WifiSampler(wifiManager);

        currentSampler = wifiSampler;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        micPermissions = false;
        locationPermissions = false;
        GPSEnabled = false;
        initializeSamplers();

        ActivityMapsBinding activityMapsBinding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(activityMapsBinding.getRoot());
        SERVICE_CHANNEL_ID = getString(R.string.notification_channel_id_background_service);
        BASIC_CHANNEL_ID = "basic";

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        createNotificationChannel();

        btn_sample = findViewById(R.id.btn_sample);
        btn_noise = findViewById(R.id.btn_noise);
        btn_signal = findViewById(R.id.btn_signal);
        btn_wifi = findViewById(R.id.btn_wifi);
        btn_resetDB = findViewById(R.id.btn_resetDB);
        btn_10m = findViewById(R.id.btn_10m);
        btn_100m = findViewById(R.id.btn_100m);
        btn_1000m = findViewById(R.id.btn_1000m);
        btn_synchData = findViewById(R.id.btn_synchData);
        btn_localSamples = findViewById(R.id.btn_phone_map);
        btn_worldSamples = findViewById(R.id.btn_world_map);

        db = new DatabaseHelper(this);

        mapGridType = GridType.HUNDRED_METER;

        //di base disabilito il bottone, lo abilito solo se il GPS è attivo e posso campionare
        btn_noise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateSampleType(SampleType.Noise);
            }
        });

        btn_signal.setOnClickListener(e -> updateSampleType(SampleType.Signal));
        btn_wifi.setOnClickListener(e -> updateSampleType(SampleType.Wifi));
        btn_sample.setOnClickListener(e -> createSample());
        btn_sample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentSampleType == SampleType.Noise) {
                    askForPermissions(Manifest.permission.RECORD_AUDIO, REQUEST_MICROPHONE);
                    if (!micPermissions) {
                        return;
                    }
                }
                if (GPSEnabled)
                    createSample();
            }
        });

        btn_resetDB.setOnClickListener(e -> {
            db.resetDB();
            localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
            drawHeatMap(localizedSampleList);
            Toast.makeText(this, "Database formattato", Toast.LENGTH_SHORT).show();
        });

        btn_synchData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadSampleFromFirebase();
                uploadMySamples();
            }
        });

        btn_10m.setOnClickListener(e -> changeGridType(GridType.TEN_METER));
        btn_100m.setOnClickListener(e -> changeGridType(GridType.HUNDRED_METER));
        btn_1000m.setOnClickListener(e -> changeGridType(GridType.KILOMETER));

        btn_localSamples.setOnClickListener(e -> {
            currentMap = "L";
            localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
            drawHeatMap(localizedSampleList);
        });
        btn_worldSamples.setOnClickListener(e -> {
            currentMap = "W";
            localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
            drawHeatMap(localizedSampleList);
        });

        //if needed, ask for permissions
        setActivityTitle("Map");
        askForPermissions(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_LOCATION);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        defaultSamplerKey = getString(R.string.settings_samplers_key);
        defaultGranularityKey = getString(R.string.settings_granularity_key);
        String defaultBackgroundSamplingKey = getString(R.string.settings_background_key);

        defaultSampler =  defaultSharedPreferences.getString(defaultSamplerKey, "null");
        defaultGranularity =  defaultSharedPreferences.getString(defaultGranularityKey, "null");
        backgroundSamplingOn = defaultSharedPreferences.getBoolean(defaultBackgroundSamplingKey, false);

        if (locationPermissions) {
            enableGPS();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }


        //firebase Init
        firebaseDB = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        stopService();
        localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
        drawHeatMap(localizedSampleList);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (backgroundSamplingOn)
            startService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (backgroundSamplingOn)
            stopService();
    }

    private void createNotificationChannel(){
        NotificationChannel serviceChannel = new NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Pangolin background service",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationChannel basicNotification = new NotificationChannel(
                BASIC_CHANNEL_ID,
                "Pangolin basic notification",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(serviceChannel);
        nm.createNotificationChannel(basicNotification);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapManager = new MapManager(googleMap, this);

        //Disegna la griglia corrispondente
        mapManager.drawGrid(mapGridType);

        if (GPSEnabled) {
            mapManager.setLocationEnabled();
            mapManager.moveCameraToUserPosition(fusedLocationClient);
            btn_sample.setEnabled(true);
        } else {
            LatLng coords = new LatLng(41.902782, 12.496366);
            mapManager.moveCameraTo(coords);
        }

        //Imposta la mappa del noise al primo avvio
        setSampler(defaultSampler);
        setGranularity(defaultGranularity);
        btn_localSamples.performClick();
    }
    private void setSampler(String s){
        switch (s){
            case "noise":
                btn_noise.performClick();
                break;
            case "wifi":
                btn_wifi.performClick();
                break;
            case "signal":
                btn_signal.performClick();
                break;
            default:
                btn_noise.performClick();
                SharedPreferences.Editor editor = defaultSharedPreferences.edit();
                editor.putString(defaultSamplerKey, "noise");
                editor.apply();
                break;
        }
    }

    private void setGranularity(String g){
        switch (g){
            case "10":
                btn_10m.performClick();
                break;
            case "100":
                btn_100m.performClick();
                break;
            case "1000":
                btn_1000m.performClick();
                break;
            default:
                btn_100m.performClick();
                SharedPreferences.Editor editor = defaultSharedPreferences.edit();
                editor.putString(defaultGranularityKey, "100");
                editor.apply();
                break;
        }
    }

    private void updateSampleType(SampleType newType){
        currentSampleType = newType;

        if (currentSampleType == SampleType.Noise)
            currentSampler = noiseSampler;
        else if (currentSampleType == SampleType.Signal)
            currentSampler = signalSampler;
        else
            currentSampler = wifiSampler;

        localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
        drawHeatMap(localizedSampleList);
    }

    private void changeGridType(GridType newGridType){
        this.mapGridType = newGridType;
        mapManager.drawGrid(newGridType);
        localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
        drawHeatMap(localizedSampleList);
    }


    private void askForPermissions(String permissions, int requestCode){
        if (ActivityCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {permissions},
                    requestCode);
            return;
        }

        if (requestCode == REQUEST_MICROPHONE)
            micPermissions = true;
        else if (requestCode == REQUEST_LOCATION) {
            locationPermissions = true;
            GPSEnabled = true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                micPermissions = true;
                btn_noise.setEnabled(true);
                Toast.makeText(this, "Mic permissions given", Toast.LENGTH_SHORT).show();
            } else {
                micPermissions = false;
            }
        }

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissions = true;
                enableGPS();
                mapManager.setLocationEnabled();
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                mapManager.moveCameraToUserPosition(fusedLocationClient);
                Toast.makeText(this, "Localization permissions given", Toast.LENGTH_SHORT).show();
            } else {
                locationPermissions = false;
                btn_sample.setEnabled(false);
            }
        }

        if (requestCode == REQUEST_ENABLE_GPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GPSEnabled = true;
                btn_sample.setEnabled(true);
                Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_ENABLE_GPS);
        }
        GPSEnabled = true;
    }


    private void loadHeatMap(){
        List<LocalizedSample> list = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
        drawHeatMap(list);
    }

    private List<LocalizedSample> getLocalSamples(String sampleType, int accuracy, int dateLimit){
        List<LocalizedSample> localizedSampleList = new ArrayList<>();
        Cursor samplesCursor = db.getAvgConditionByAccuracy(sampleType, accuracy, dateLimit, currentMap);

        while (samplesCursor.moveToNext()){
            //prendo la coordinata
            String gridzone = samplesCursor.getString(0);
            String square = samplesCursor.getString(1);
            String easting = samplesCursor.getString(2);
            String northing = samplesCursor.getString(3);
            //prendo la condizione
            int avg_cond = Math.round(samplesCursor.getFloat(4));

            String coordString = gridzone + square + easting + northing;
            MGRS MGRScoord = MGRSTools.fromStringToMGRS(coordString);
            Sample s = new Sample(SampleType.valueOf(sampleType), avg_cond, 0);
            LocalizedSample ls = new LocalizedSample(s, coordString, null);
            localizedSampleList.add(ls);
        }

        samplesCursor.close();

        return localizedSampleList;
    }

    private void drawHeatMap(List<LocalizedSample> sampleList){
        if (currentSampleType == null) {
            Toast.makeText(this, "Something went wrong while maps' drawing", Toast.LENGTH_SHORT).show();
            return;
        }

        mapManager.deleteAllPolygons();

        for (LocalizedSample ls : sampleList){
            MGRS mgrsCoord = MGRSTools.fromStringToMGRS(ls.getMgrsCoords());
            int condition = ls.getBasicSample().getCondition();
            mapManager.colorTile(mgrsCoord, getColorByValue(condition), mapGridType, currentSampleType);
        }
    }
    private int getColorByValue(int val){
        if (val == 0)
            return Color.rgb(255,0,0);
        else if (val == 1)
            return Color.rgb(255,255,0);
        else
            return Color.rgb(0, 255, 0);
    }

    @SuppressLint("MissingPermission")
    private void createSample() {
        if (noiseSampler == null || signalSampler == null || wifiSampler == null) {
            Toast.makeText(this, "Sampler non inizializzato", Toast.LENGTH_SHORT).show();
            return;
        }

        Sample newSample = currentSampler.getSample();

        if (newSample == null){
            Toast.makeText(this, "Errore riscontrato durante il sampling, riprovare", Toast.LENGTH_SHORT).show();
            return;
        }
        String msgToShow = "Value: " + newSample.getValue() + " Condition: " + newSample.getCondition();
        Toast.makeText(this,msgToShow, Toast.LENGTH_SHORT).show();
        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng cor = new LatLng(location.getLatitude(), location.getLongitude());
                        MGRS currentLocation = mapManager.getMGRS(cor);

                        String zone = currentLocation.getZone() + "" + currentLocation.getBand();
                        String square = currentLocation.getColumnRowId();
                        String easting = MGRSTools.getMaxAccuracyEN(currentLocation.getEasting());
                        String northing = MGRSTools.getMaxAccuracyEN(currentLocation.getNorthing());

                        //System.out.println("Added coord: " + currentLocation.toString());
                        System.out.println("Added coord: "+ zone + " " + square + " " + easting + " " + northing);
                        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

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
                        localizedSampleList = getLocalSamples(currentSampleType.toString(), mapGridType.getAccuracy(), 100);
                        drawHeatMap(localizedSampleList);
                    }
                });
    }

    private void startService(){
        Intent serviceIntent = new Intent(this, BackgroundSamplerService.class);
        startService(serviceIntent);
    }

    private void stopService(){
        Intent serviceIntent = new Intent(this, BackgroundSamplerService.class);
        stopService(serviceIntent);
    }

    private Timestamp getTimestampFromString(String ts){
        Date d = new Date();
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            d = sdf.parse(ts);
            return new Timestamp(d);
        } catch (ParseException e) {
            System.out.println("Error while parsing data");
        }
        return  null;
    }

    private void uploadMySamples(){
        CollectionReference collection = firebaseDB.collection("samples");
        Cursor samplesToSynch = db.getSamplesToSynch();

        while(samplesToSynch.moveToNext()){
            String type = samplesToSynch.getString(0);
            String timestampString = samplesToSynch.getString(1);

            Timestamp ts = getTimestampFromString(timestampString);

            double value = samplesToSynch.getDouble(2);
            int condition = samplesToSynch.getInt(3);
            String gridzone = samplesToSynch.getString(4);
            String square = samplesToSynch.getString(5);
            String easting = samplesToSynch.getString(6);
            String northing = samplesToSynch.getString(7);
            String id = samplesToSynch.getString(8);
            System.out.println("Uploading sample: " + id);
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("id", id);
            sampleData.put("type", type);
            sampleData.put("value", value);
            sampleData.put("condition", condition);
            sampleData.put("gridzone", gridzone);
            sampleData.put("square", square);
            sampleData.put("easting", easting);
            sampleData.put("northing", northing);
            sampleData.put("timestamp", ts);


            collection.add(sampleData).addOnSuccessListener(documentReference -> {
                        System.out.println("Added correctly " + id);
                        db.updateSampleState(id, "SYNC");
                    })
                    .addOnFailureListener(e -> {
                        // Handle errors
                        System.out.println("Error while adding a sample ");
                    });
        }
    }

    private void downloadSampleFromFirebase(){
        CollectionReference collection = firebaseDB.collection("samples");
        long lastUpdate =
                defaultSharedPreferences.getLong(getString(R.string.last_synch), 1282983341);

        Timestamp timestamp = new Timestamp(new Date(lastUpdate));

        Query query = collection.whereGreaterThan("timestamp", timestamp);
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        query.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot d : queryDocumentSnapshots.getDocuments()){
                    String type = d.getString("type");
                    double conditionD = d.getDouble("condition");
                    int cond = (int) conditionD;
                    double value =  d.getDouble("value");
                    String gridzone = d.getString("gridzone");
                    String square = d.getString("square");
                    String easting = d.getString("easting");
                    String northing = d.getString("northing");
                    String coords = gridzone + square + easting + northing;
                    String id = d.getString("id");
                    Date timestampRaw = d.getDate("timestamp");
                    // Convert Date to formatted string
                    String timestamp = sdf.format(timestampRaw);

                    db.addSampleFromFirebase(
                            type,
                            timestamp,
                            value,
                            cond,
                            gridzone,
                            square,
                            easting,
                            northing,
                            id,
                            "W"
                    );
                }
            }
        });

        //aggiorno il momento dell'ultimo synch
        long currentMoment = System.currentTimeMillis();
        SharedPreferences.Editor editor = defaultSharedPreferences.edit();
        editor.putLong(getString(R.string.last_synch), currentMoment);
        editor.apply();

    }

}
