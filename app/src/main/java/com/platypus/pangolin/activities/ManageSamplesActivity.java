package com.platypus.pangolin.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.platypus.pangolin.R;
import com.platypus.pangolin.adapters.RecycleViewInterface;
import com.platypus.pangolin.adapters.SamplesAdapter;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.databinding.ActivityManageSamplesBinding;
import com.platypus.pangolin.models.LocalizedSample;
import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class ManageSamplesActivity extends DrawerBaseActivity implements RecycleViewInterface {
    ActivityManageSamplesBinding activityManageSamplesBinding;
    Spinner sampleTypeSpinner;
    RecyclerView samplesListView;

    DatabaseHelper db;
    RecyclerView.LayoutManager sampleListLayoutManager;

    SamplesAdapter samplesAdapter;
    ArrayList<LocalizedSample> samplesArrayList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DatabaseHelper(this);
        activityManageSamplesBinding = ActivityManageSamplesBinding.inflate(getLayoutInflater());
        setContentView(activityManageSamplesBinding.getRoot());
        setActivityTitle("Manage samples");

        sampleTypeSpinner = findViewById(R.id.spinner2);
        samplesListView = findViewById(R.id.sampleList);
        sampleListLayoutManager = new LinearLayoutManager(this);


        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.sample_types,
                android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        sampleTypeSpinner.setAdapter(adapter);
        String selectedItem = (String) sampleTypeSpinner.getSelectedItem();
        samplesArrayList = getSamples(selectedItem);
        samplesAdapter = new SamplesAdapter(samplesArrayList, this);
        samplesListView.setAdapter(samplesAdapter);
        samplesListView.setLayoutManager(sampleListLayoutManager);

        sampleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedItem = adapterView.getSelectedItem().toString();
                samplesArrayList = getSamples(selectedItem);
                samplesAdapter.setSamples(samplesArrayList);
                samplesListView.setAdapter(samplesAdapter);
                samplesListView.setLayoutManager(sampleListLayoutManager);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        samplesArrayList = getSamples((String) sampleTypeSpinner.getSelectedItem());
        samplesAdapter.setSamples(samplesArrayList);
        samplesListView.setAdapter(samplesAdapter);
        samplesListView.setLayoutManager(sampleListLayoutManager);
    }

    private ArrayList<LocalizedSample> getSamples(String sampleType){
        ArrayList<LocalizedSample> samples = new ArrayList<>();

        Cursor query = db.getSamplesByType(sampleType);

        while(query.moveToNext()){
            String type = query.getString(0);
            String timestamp = query.getString(1);
            float value = query.getFloat(2);
            int condition = query.getInt(3);
            String gridzone = query.getString(4);
            String square = query.getString(5);
            String easting = query.getString(6);
            String northing = query.getString(7);

            String coords = gridzone + square + easting + northing;

            Sample s = new Sample(SampleType.valueOf(type), condition, value);
            LocalizedSample ls = new LocalizedSample(s, coords, timestamp);

            samples.add(ls);
        }

        return samples;
    }

    @Override
    public void onItemClick(int position) {
        Intent i = new Intent(this, SampleViewActivity.class);
        i.putExtra("LocalizedSample", samplesArrayList.get(position));
        startActivity(i);
    }
}