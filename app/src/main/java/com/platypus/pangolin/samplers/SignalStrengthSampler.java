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

    public SignalStrengthSampler(Context context){
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private double getData(){
        List<CellSignalStrength> cellSignals = telephonyManager.getSignalStrength().getCellSignalStrengths();

        //get the signal of the current SIM
        CellSignalStrength signalStrength = cellSignals == null ? cellSignals.get(0) : null;

        if (signalStrength == null)
                return 0;

        double rssi;

        if (signalStrength instanceof CellSignalStrengthLte) {
            CellSignalStrengthLte lteSignal = (CellSignalStrengthLte) signalStrength;
            rssi = lteSignal.getRssi();
        } else if (signalStrength instanceof CellSignalStrengthCdma) {
            CellSignalStrengthCdma lteSignal = (CellSignalStrengthCdma) signalStrength;
            rssi = lteSignal.getCdmaDbm();
        } else if (signalStrength instanceof CellSignalStrengthWcdma) {
            CellSignalStrengthWcdma lteSignal = (CellSignalStrengthWcdma) signalStrength;
            rssi = lteSignal.getDbm();
        } else {
            rssi = signalStrength.getDbm();

        }

        return rssi;
    }

    private SignalCondition getLteCondition(double rssi){
        if (rssi >= -65)
            return SignalCondition.Excellent;
        if (rssi >= -70)
            return SignalCondition.Good;
        else
            return  SignalCondition.Poor;
    }

    private SignalCondition getCdmaCondition(double rssi){
        if (rssi >= -70)
            return SignalCondition.Excellent;
        if (rssi >= -95)
            return SignalCondition.Good;
        else
            return  SignalCondition.Poor;
    }

    private SignalCondition getGenericCondition(double rssi){
        if (rssi >= -60)
            return SignalCondition.Excellent;
        if (rssi >= -80)
            return SignalCondition.Good;
        else
            return  SignalCondition.Poor;
    }


    @Override
    public Sample getSample() {
        List<CellSignalStrength> cellSignals = telephonyManager.getSignalStrength().getCellSignalStrengths();

        //get the signal of the current SIM
        CellSignalStrength signalStrength = cellSignals.size() != 0 ? cellSignals.get(0)  : null;

        if (signalStrength == null)
            return null;

        double rssi;
        SignalCondition condition;

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

        //if rssi > 0 the sample something went wrong
        if (rssi > 0)
                return  null;
        return new Sample(SampleType.Signal, condition, rssi);
    }
}
