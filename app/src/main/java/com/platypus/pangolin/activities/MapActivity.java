package com.platypus.pangolin.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

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
import com.platypus.pangolin.R;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.utils.MGRSTools;

import java.util.ArrayList;
import java.util.List;

import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.MGRSUtils;
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

    MGRSTileProvider tileProvider;

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

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //codice mio
        checkLocationPermissions();
        enableGPS();
        //creo tutti gli oggetti necessari alla localizzazione
        initializeLocationServices();
    }

    private void setUpMap(){
        tileProvider = MGRSTileProvider.create(this, GridType.GZD, GridType.HUNDRED_METER);
        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
    }

    @SuppressLint("MissingPermission")
    private void setUpCamera(){
        if (hasGPSPermissios) {
            mMap.setMyLocationEnabled(true);
            //la prima volta che apri la mappa, ti porta sulla tua posizione
            fusedLocationClient
                    .getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                            colorTile(pos, Color.rgb(250,0,0));
                            CameraPosition cp = new CameraPosition.Builder()
                                    .target(pos)
                                    .zoom(16)
                                    .build();
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
                        }
                    });
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setUpMap();
        loadHeatMap(SampleType.Noise);
        setUpCamera();

        colorTile(new LatLng(46.428709, 12.375297), Color.rgb(0,255,0));
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

    private void colorTile(LatLng coords, int color){
        MGRS mgrsCoord = tileProvider.getMGRS(coords);
        colorTile(mgrsCoord, color);
    }

    private void colorTile(MGRS coords, int color) {
        if (mMap == null || tileProvider == null)
            return;

        PolygonOptions currentTile = new PolygonOptions().addAll(MGRSTools.calculateTileCorners(coords, GridType.HUNDRED_METER));
        Polygon p = mMap.addPolygon(currentTile);
        p.setStrokeWidth(0);
        p.setFillColor(color);
    }

    private void loadHeatMap(SampleType type){
        if (mMap == null) {
            Log.e("MAPERROR", "Map is null");
            return;
        }

        //TODO
        //meccanismo che pesca i dati dal DB e crea le tile....
    }
}
