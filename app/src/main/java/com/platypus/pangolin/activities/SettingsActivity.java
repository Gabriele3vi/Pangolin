package com.platypus.pangolin.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.platypus.pangolin.databinding.ActivitySettingsBinding;

public class SettingsActivity extends DrawerBaseActivity {
    ActivitySettingsBinding activitySettingsBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activitySettingsBinding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(activitySettingsBinding.getRoot());

        setActivityTitle("Settings");
    }
}