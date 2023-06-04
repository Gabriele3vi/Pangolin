package com.platypus.pangolin.samplers;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.models.SignalCondition;

public class WifiSampler extends Sampler{
    WifiManager wifiManager;
    public WifiSampler(Context context){
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    }
    @Override
    public Sample getSample() {
        if (!wifiManager.isWifiEnabled())
            return null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        double rssi = wifiInfo.getRssi();
        SignalCondition condition;

        if (rssi >= -50)
            condition = SignalCondition.Excellent;
        else if (rssi >= -65)
            condition = SignalCondition.Good;
        else
            condition = SignalCondition.Poor;

        return new Sample(SampleType.Wifi, condition, rssi);
    }
}
