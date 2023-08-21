package com.platypus.pangolin.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.platypus.pangolin.R;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.models.LocalizedSample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.utils.MGRSTools;

import mil.nga.mgrs.MGRS;

public class SampleViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    TextView dateTV, valueTV, conditionTV;
    Button btnDeleteSample;
    LocalizedSample currentSample;
    DatabaseHelper db;

    LatLng coords;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = getIntent();
        currentSample = (LocalizedSample) data.getSerializableExtra("LocalizedSample");

        setContentView(R.layout.activity_sample_view);
        db = new DatabaseHelper(this);
        dateTV = findViewById(R.id.timestamp_tv);
        valueTV = findViewById(R.id.value_tv);
        conditionTV = findViewById(R.id.condition_tv);
        btnDeleteSample = findViewById(R.id.btn_delete_sample);

        btnDeleteSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int row = db.deleteSample(currentSample.getTimestamp(), currentSample.getBasicSample().getType().toString());
                System.out.println("Deleted rows: " + row);
                finish();
            }
        });

        dateTV.setText(currentSample.getTimestamp());
        valueTV.setText(currentSample.getBasicSample().getValue() + " db");
        int condition = currentSample.getBasicSample().getCondition();

        if (condition == 0) {
            conditionTV.setText("Poor");
            conditionTV.setTextColor(Color.RED);
        }
        else if  (condition == 1) {
            conditionTV.setText("Average");
            conditionTV.setTextColor(Color.rgb(255,140,0));
        }
        else {
            conditionTV.setText("Excellent");
            conditionTV.setTextColor(Color.GREEN);
        }

        MGRS mgrsCoords = MGRSTools.fromStringToMGRS(currentSample.getMgrsCoords());
        System.out.println(currentSample.getMgrsCoords());
        coords = MGRSTools.fromMGRStoLatLng(mgrsCoords);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.single_sample_map);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(coords).title("Sample"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, 15));
    }
}