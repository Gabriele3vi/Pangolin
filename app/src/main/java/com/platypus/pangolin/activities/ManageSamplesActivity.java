package com.platypus.pangolin.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.platypus.pangolin.databinding.ActivityManageSamplesBinding;

public class ManageSamplesActivity extends DrawerBaseActivity {
    ActivityManageSamplesBinding activityManageSamplesBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityManageSamplesBinding = ActivityManageSamplesBinding.inflate(getLayoutInflater());
        setContentView(activityManageSamplesBinding.getRoot());
        setActivityTitle("Manage samples");
    }
}