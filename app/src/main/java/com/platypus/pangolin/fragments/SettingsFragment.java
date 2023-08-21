package com.platypus.pangolin.fragments;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.platypus.pangolin.R;
import com.platypus.pangolin.services.BackgroundSamplerService;


public class SettingsFragment extends PreferenceFragmentCompat {
    Context c = getContext();
    private final int REQUEST_POST_NOTIFICATION = 5;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.myprefs, rootKey);

        Preference notificationPreference = findPreference(getString(R.string.settings_notification_key));
        Preference backgroundPreference = findPreference(getString(R.string.settings_background_key));

        notificationPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean pref = (Boolean) newValue;
                System.out.println("Notification set: " + newValue);

                if (pref) {
                    askForNotificationPermissions();
                    //System.out.println(c);
                }
                return true;
            }
        });

        backgroundPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean pref = (Boolean) newValue;
                System.out.println("Notification set: " + newValue);
                Intent serviceIntent = new Intent(getActivity(), BackgroundSamplerService.class);

                if (pref) {
                    getActivity().startService(serviceIntent);
                    System.out.println("Started sampling");

                } else {
                    getActivity().stopService(serviceIntent);
                    System.out.println("Stopped sampling");
                }
                return true;
            }
        });
    }

    private void askForNotificationPermissions(){
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_POST_NOTIFICATION);
            System.out.println("Autorizzazione richiesta");
        }
        System.out.println("Fine del metodo");
    }
}
