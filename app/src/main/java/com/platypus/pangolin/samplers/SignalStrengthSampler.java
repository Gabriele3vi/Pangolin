package com.platypus.pangolin.samplers;

import android.content.Context;
import android.os.Build;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.models.SignalCondition;

import java.util.List;

public class SignalStrengthSampler extends Sampler {
    private TelephonyManager telephonyManager;

    public SignalStrengthSampler(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    private int getLteCondition(double rssi){
        return getCondition(-65, -70, rssi);
    }

    private int getCdmaCondition(double rssi){
        return getCondition(-70, -95, rssi);
    }

    private int getGenericCondition(double rssi){
        return getCondition(-60, -80, rssi);
    }

    //Dato che in base al tipo di rete a cui sono collegato il RSSI ha un significato diverso,
    //questo metodo serve per non scrivere troppo codice ripetuto
    //
    private int getCondition(int excellentBound, int goodBound, double rssi) {
        if (rssi >= excellentBound)
            return SignalCondition.EXCELLENT;
        if (rssi >= goodBound)
            return SignalCondition.GOOD;
        else
            return  SignalCondition.POOR;
    }


    @Override
    public Sample getSample() {
        List<CellSignalStrength> cellSignals = telephonyManager.getSignalStrength().getCellSignalStrengths();

        //get the signal of the current SIM
        CellSignalStrength signalStrength = cellSignals.size() != 0 ? cellSignals.get(0)  : null;

        if (signalStrength == null)
            return null;

        double rssi;
        int condition;

        if (signalStrength instanceof CellSignalStrengthLte) {
            CellSignalStrengthLte lteSignal = (CellSignalStrengthLte) signalStrength;
            rssi = lteSignal.getRssi();
            condition = getLteCondition(rssi);
        } else if (signalStrength instanceof CellSignalStrengthCdma) {
            CellSignalStrengthCdma lteSignal = (CellSignalStrengthCdma) signalStrength;
            rssi = lteSignal.getCdmaDbm();
            condition = getCdmaCondition(rssi);
        } else {
            rssi = signalStrength.getDbm();
            condition = getGenericCondition(rssi);
        }

        //if rssi > 0 something went wrong
        if (rssi > 0)
                return  null;
        return new Sample(SampleType.Signal, condition, rssi);
    }
}
