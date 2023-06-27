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
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.platypus.pangolin.R;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.samplers.AcousticNoiseSampler;
import com.platypus.pangolin.samplers.Sampler;
import com.platypus.pangolin.samplers.SignalStrengthSampler;
import com.platypus.pangolin.samplers.WifiSampler;
import com.platypus.pangolin.utils.MGRSTools;
import com.platypus.pangolin.utils.MapManager;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{
    //AIzaSyAwhBgttUCkbyHTYGSL49hZFSmESdli4cM api key
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;
    private static final int REQUEST_ENABLE_GPS = 1001;

    private GoogleMap mMap;
    private boolean hasGPSPermissios;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton btn_sample, btn_resetDB;
    private Button btn_noise, btn_signal, btn_wifi;
    private Button btn_10m, btn_100m, btn_1000m;
    private SampleType currentSampleType;
    private Sampler noiseSampler, signalSampler, wifiSampler, currentSampler;
    private GridType mapGridType;
    private DatabaseHelper db;
    private MapManager mapManager;

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //codice per gestire il drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationView);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.nav_map:
                        Toast.makeText(MapActivity.this, "Map", Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.nav_data:
                        Toast.makeText(MapActivity.this, "Samples", Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.nav_settings:
                        Toast.makeText(MapActivity.this, "Settings", Toast.LENGTH_SHORT).show();
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                }
                return true;
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btn_sample = findViewById(R.id.btn_sample);
        btn_noise = findViewById(R.id.btn_noise);
        btn_signal = findViewById(R.id.btn_signal);
        btn_wifi = findViewById(R.id.btn_wifi);
        btn_resetDB = findViewById(R.id.btn_resetDB);
        btn_10m = findViewById(R.id.btn_10m);
        btn_100m = findViewById(R.id.btn_100m);
        btn_1000m = findViewById(R.id.btn_1000m);

        db = new DatabaseHelper(this);

        mapGridType = GridType.HUNDRED_METER;

        //di base disabilito il bottone, lo abilito solo se il GPS è attivo e posso campionare
        btn_sample.setEnabled(false);
        btn_noise.setOnClickListener(e -> updateSampleType(SampleType.Noise));
        btn_signal.setOnClickListener(e -> updateSampleType(SampleType.Signal));
        btn_wifi.setOnClickListener(e -> updateSampleType(SampleType.Wifi));
        btn_sample.setOnClickListener(e -> createSample());
        btn_resetDB.setOnClickListener(e -> {
            db.resetDB();
            Toast.makeText(this, "Database formattato", Toast.LENGTH_SHORT).show();
        });

        btn_10m.setOnClickListener(e -> changeGridType(GridType.TEN_METER));
        btn_100m.setOnClickListener(e -> changeGridType(GridType.HUNDRED_METER));
        btn_1000m.setOnClickListener(e -> changeGridType(GridType.KILOMETER));

        checkLocationPermissions();
        enableGPS();
        //creo tutti gli oggetti necessari alla localizzazione
        initializeLocationServices();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapManager = new MapManager(googleMap, this);

        //Disegna la griglia corrispondente
        mapManager.drawGrid(mapGridType);

        if (hasGPSPermissios) {
            mapManager.setLocationEnabled();
            mapManager.moveCameraToUserPosition(fusedLocationClient);
            initializeSamplers();
            btn_sample.setEnabled(true);
        }

        //Imposta la mappa del noise al primo avvio
        btn_noise.performClick();
        btn_100m.performClick();
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

    private void changeGridType(GridType newGridType){
        this.mapGridType = newGridType;
        mapManager.drawGrid(newGridType);
        loadHeatMap();
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
        if(requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasGPSPermissios = true;
            }
        }

        if (requestCode == REQUEST_ENABLE_GPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasGPSPermissios = true;
            } else {
                hasGPSPermissios = false;
            }
        }
    }

    private void enableGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_ENABLE_GPS);
        }
    }


    private void loadHeatMap(){
        if (currentSampleType == null) {
            Log.e("MAPERROR", "Somthing went wrong");
            Toast.makeText(this, "Something went wrong while maps' loading", Toast.LENGTH_SHORT).show();
            return;
        }

        //Toglie dalla mappa i quadranti precedenti
        mapManager.deleteAllPolygons();

        //Prendo i dati dal database
        Cursor samplesCursor = db.getAvgConditionByAccuracy(
                currentSampleType.toString(),
                mapGridType.getAccuracy(),
                100
        );

        if (samplesCursor == null){
            Toast.makeText(this, "Error while retriving data", Toast.LENGTH_SHORT).show();
            return;
        }

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
            //coloro il quadrato corrispondente
            mapManager.colorTile(MGRScoord, getColorByValue(avg_cond), mapGridType);
            //System.out.println("Added coord loading map: " + gridzone + square + easting + northing );
        }

        samplesCursor.close();
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
                    }
                });
        loadHeatMap();
    }
}
