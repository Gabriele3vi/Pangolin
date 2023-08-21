package com.platypus.pangolin.samplers;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.models.SignalCondition;

public class WifiSampler extends Sampler{
    WifiManager wifiManager;

    public WifiSampler(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }
    @Override
    public Sample getSample() {
        if (!wifiManager.isWifiEnabled())
            return null;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        double rssi = wifiInfo.getRssi();
        int condition;

        if (rssi >= -50)
            condition = SignalCondition.EXCELLENT;
        else if (rssi >= -65)
            condition = SignalCondition.GOOD;
        else
            condition = SignalCondition.POOR;

        return new Sample(SampleType.Wifi, condition, rssi);
    }
}
