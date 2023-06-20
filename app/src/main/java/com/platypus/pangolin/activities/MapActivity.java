package com.platypus.pangolin.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.platypus.pangolin.R;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.samplers.AcousticNoiseSampler;
import com.platypus.pangolin.samplers.Sampler;
import com.platypus.pangolin.samplers.SignalStrengthSampler;
import com.platypus.pangolin.samplers.WifiSampler;
import com.platypus.pangolin.utils.MGRSTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.tile.MGRSTileProvider;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{
    //AIzaSyAwhBgttUCkbyHTYGSL49hZFSmESdli4cM api key
    private static final int REQUEST_ENABLE_GPS = 1001;

    private GoogleMap mMap;
    private boolean hasGPSPermissios;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;

    private FloatingActionButton btn_sample, btn_resetDB;

    private Button btn_noise, btn_signal, btn_wifi;

    private SampleType currentSampleType;

    private Sampler noiseSampler, signalSampler, wifiSampler, currentSampler;
    private List<Polygon> polygonList;

    private MGRSTileProvider tileProvider;

    private GridType mapGridType;
    private DatabaseHelper db;

    private HashMap<String, ArrayList<Sample>> localizedSamples;

    private void initializeLocationServices(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
        };
    }

    private void updateSampleType(SampleType newType){
        currentSampleType = newType;

        if (currentSampleType == SampleType.Noise)
            currentSampler = noiseSampler;
        else if (currentSampleType == SampleType.Signal)
            currentSampler = signalSampler;
        else
            currentSampler = wifiSampler;

        loadHeatMap();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btn_sample = findViewById(R.id.btn_sample);
        btn_noise = findViewById(R.id.btn_noise);
        btn_signal = findViewById(R.id.btn_signal);
        btn_wifi = findViewById(R.id.btn_wifi);
        btn_resetDB = findViewById(R.id.btn_resetDB);
        db = new DatabaseHelper(this);

        mapGridType = GridType.HUNDRED_METER;

        polygonList = new ArrayList<>();

        //di base disabilito il bottone, lo abilito solo se il GPS è attivo e posso campionare
        btn_sample.setEnabled(false);
        btn_noise.setOnClickListener(e -> updateSampleType(SampleType.Noise));
        btn_signal.setOnClickListener(e -> updateSampleType(SampleType.Signal));
        btn_wifi.setOnClickListener(e -> updateSampleType(SampleType.Wifi));
        btn_sample.setOnClickListener(e -> createSample());
        btn_resetDB.setOnClickListener(e -> {
            db.resetDB();
            Toast.makeText(this, "D", Toast.LENGTH_SHORT).show();
        });

        checkLocationPermissions();
        enableGPS();
        //creo tutti gli oggetti necessari alla localizzazione
        initializeLocationServices();
    }


    @SuppressLint("MissingPermission")
    private void setUpCamera(){
        mMap.setMyLocationEnabled(true);
        //la prima volta che apri la mappa, ti porta sulla tua posizione
        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                        //colorTile(pos, Color.rgb(250,0,0), mapGridType);
                        CameraPosition cp = new CameraPosition.Builder()
                                .target(pos)
                                .zoom(16)
                                .build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
                    }
                });

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Disegna la griglia corrispondente
        tileProvider = MGRSTileProvider.create(this, GridType.GZD, mapGridType);
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));

        if (hasGPSPermissios) {
            setUpCamera();
            initializeSamplers();
            btn_sample.setEnabled(true);
        }

        //Imposta la mappa del noise di default
        btn_noise.performClick();

        //colorTile(new LatLng(44.498955, 11.327591), Color.rgb(0,255,0), mapGridType);
    }

    private void initializeSamplers() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        this.noiseSampler = new AcousticNoiseSampler(500);
        this.signalSampler = new SignalStrengthSampler(telephonyManager);
        this.wifiSampler = new WifiSampler(wifiManager);

        currentSampler = wifiSampler;
    }


    private void checkLocationPermissions(){
        hasGPSPermissios = true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            hasGPSPermissios = false;
            ActivityCompat.requestPermissions(
                    MapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            hasGPSPermissios = true;
        }
    }

    private void enableGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_ENABLE_GPS);
        }
    }

    private void colorTile(LatLng coords, int value, GridType gridType){
        MGRS mgrsCoord = tileProvider.getMGRS(coords);
        colorTile(mgrsCoord, value ,gridType);
    }

    private void colorTile(MGRS coords, int color, GridType gridType) {
        if (mMap == null || tileProvider == null)
            return;

        PolygonOptions currentTile = new PolygonOptions().addAll(MGRSTools.calculateTileCorners(coords, gridType));
        Polygon p = mMap.addPolygon(currentTile);
        p.setStrokeWidth(0);
        p.setFillColor(color);

        polygonList.add(p);
    }


    private void loadHeatMap(){
        if (mMap == null || currentSampleType == null) {
            Log.e("MAPERROR", "Somthing went wrong");
            Toast.makeText(this, "Something went wrong while maps' loading", Toast.LENGTH_SHORT).show();
            return;
        }

        //Toglie dalla mappa i quadranti precedenti
        for (Polygon p : polygonList){
            p.remove();
        }

        polygonList.clear();

        //ricreo l'hashmap
        localizedSamples = new HashMap<>();

        //creo il cursore per lavorare col DB
        Cursor samplesCursor = db.getSamplesByType(this.currentSampleType);

        if (samplesCursor == null){
            Toast.makeText(this, "Error while retriving data", Toast.LENGTH_SHORT).show();
            return;
        }

        //estraggo i dati dal database
        while (samplesCursor.moveToNext()){
            //prendo i dati dal DB
            String data = samplesCursor.getString(0);
            double val = samplesCursor.getDouble(1);
            int condition = samplesCursor.getInt(2);
            String coordCasted = MGRSTools.castMGRSCoord(samplesCursor.getString(3), mapGridType.getAccuracy());
            Sample currentSample = new Sample(currentSampleType, condition, val, data);
            System.out.println("DB coord: " + samplesCursor.getString(3));
            //aggiungo l'elemento nella casella corretta dall'hashmap
            localizedSamples.computeIfAbsent(coordCasted, k -> new ArrayList<>());
            localizedSamples.get(coordCasted).add(currentSample);
        }

        samplesCursor.close();

        //itero sull'hashmap, calcolo la media e disegno la tile
        for (Map.Entry<String, ArrayList<Sample>> entry : localizedSamples.entrySet()){
            //prendo i vari dati
            String coord = entry.getKey();
            ArrayList<Sample> samples = entry.getValue();
            Collections.sort(samples);

            //callcolo la media fermandomi all'impostazione scelta dall'utente
            int sum = 0;
            int maxSamples = 5;

            for (int i = 0; i < samples.size() && i < maxSamples; i++){
                sum += samples.get(i).getCondition();
            }

            int mean = Math.round(sum / samples.size());
            System.out.println("Raw coord: " + coord);
            MGRS cordsMGRS = MGRSTools.fromStringToMGRS(coord, mapGridType.getAccuracy());
            System.out.println("MGRS object coord: " + cordsMGRS.toString());

            colorTile(cordsMGRS, getColorByValue(mean), mapGridType);
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
            Toast.makeText(this, "Errore durante il sampling", Toast.LENGTH_SHORT).show();
            return;
        }

        Sample newSample = currentSampler.getSample();

        if (newSample == null){
            Toast.makeText(this, "Errore riscontrato durante il sampling, riprovare", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng cor = new LatLng(location.getLatitude(), location.getLongitude());
                        MGRS currentLocation = tileProvider.getMGRS(cor);

                        Log.d("COORD2", currentLocation.toString());
                        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        db.addSample(
                                newSample.getType().toString(),
                                newSample.getTimeStamp(),
                                newSample.getValue(),
                                newSample.getCondition(),
                                currentLocation.toString()
                                );
                    }
                });
        loadHeatMap();
    }
}
