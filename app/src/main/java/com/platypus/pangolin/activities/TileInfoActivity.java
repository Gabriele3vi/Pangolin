package com.platypus.pangolin.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.platypus.pangolin.R;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.utils.MGRSTools;

import java.util.ArrayList;
import java.util.List;

import mil.nga.mgrs.MGRS;

public class TileInfoActivity extends AppCompatActivity {

    private TextView tv;
    private DatabaseHelper db;

    private LineChart lineChart;
    private List<String> xValues;
    private List<Float> yValues;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile_info);
        tv = findViewById(R.id.tv_test_2);
        db = new DatabaseHelper(this);
        lineChart = findViewById(R.id.lineChart);

        //Prendo le informazioni che servono per creare il grafico
        Intent intentData = getIntent();
        String [] data = intentData.getStringExtra("Data").split("_");
        MGRS coords = MGRSTools.fromStringToMGRS(data[0]);
        String sampleType = data[1];
        String gridAccuracy = data[2];

        loadChartData(coords, gridAccuracy, sampleType);

        //imposto il grafico
        Description desc = new Description();
        desc.setEnabled(false);
        lineChart.setDescription(desc);
        lineChart.getAxisRight().setDrawLabels(false);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues));
        xAxis.setLabelCount(Math.min(xValues.size(), 10));
        xAxis.setGranularity(1f);

        YAxis yAxis = lineChart.getAxisLeft();
        float axisMinimum = (float) (getMin(yValues) - 0.1 * getMin(yValues));
        yAxis.setAxisMinimum(axisMinimum);

        float axisMaximum = (float) (getMax(yValues) + 0.1 * getMax(yValues));
        yAxis.setAxisMaximum(axisMaximum);
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.RED);
        yAxis.setLabelCount(10);

        List<Entry> entryList = generateEntryDataset();
        LineDataSet dataset = new LineDataSet(entryList, sampleType);
        dataset.setColor(Color.BLUE);
        LineData lineData = new LineData(dataset);
        lineChart.setData(lineData);
        lineChart.invalidate();

    }

    private void loadChartData(MGRS coords, String gridAccuracy, String sampleType){
        xValues = new ArrayList<>();
        yValues = new ArrayList<>();
        String zone = coords.getZone() + "" + coords.getBand();
        String square = coords.getColumnRowId();
        String easting = MGRSTools.getMaxAccuracyEN(coords.getEasting());
        String northing = MGRSTools.getMaxAccuracyEN(coords.getNorthing());

        Cursor samplesCursor = db.getSamplesByCoordAndAccuracyAndType(zone, square, easting, northing, sampleType, gridAccuracy, "W");

        while (samplesCursor.moveToNext()){
            //prendo la coordinata
            String timestamp = samplesCursor.getString(0).replace(" ", "\n");
            float value = samplesCursor.getFloat(1);
            int condition = samplesCursor.getInt(2);
            //prendo la condizione
            xValues.add(timestamp);
            yValues.add(Math.abs(value));
            System.out.println(timestamp + " value: " + value + " condition: " + condition);
        }

        db.close();
    }

    private List<Entry> generateEntryDataset(){
        List<Entry> entryList = new ArrayList<>();
        int counter = 0;
        for(Float val : yValues){
            entryList.add(new Entry(counter, val));
            counter++;
        }

        return entryList;
    }

    private float getMax(List<Float> list){
        float max = 0;
        for (float f : list){
            if (f > max)
                max = f;
        }
        return max;
    }

    private float getMin(List<Float> list){
        float min = list.get(0);
        for (float f : list){
            if (f < min)
                min = f;
        }
        return min;
    }


}