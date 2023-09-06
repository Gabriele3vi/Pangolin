package com.platypus.pangolin.samplers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.core.app.ActivityCompat;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.models.SignalCondition;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

public class WifiSampler extends Sampler {
    WifiManager wifiManager;

    public WifiSampler(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    @Override
    public Sample getSample() {
        if (!wifiManager.isWifiEnabled())
            return null;

        int mean = 0;

        @SuppressLint("MissingPermission")
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for (ScanResult accessPoint: scanResults) {
            System.out.println(accessPoint);
            System.out.println(accessPoint.level);
            mean += accessPoint.level;
        }


        mean =  (mean / scanResults.size());

        int condition;

        //se non sono presenti wifi, wifi pessimo
        if (mean == 0) {
            mean = -127;
        }

        if (mean >= -50)
            condition = SignalCondition.EXCELLENT;
        else if (mean >= -65)
            condition = SignalCondition.GOOD;
        else
            condition = SignalCondition.POOR;

        return new Sample(SampleType.Wifi, condition, mean);
    }
}
