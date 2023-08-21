package com.platypus.pangolin.models;

import com.google.android.gms.maps.model.LatLng;
import com.platypus.pangolin.utils.MGRSTools;

import java.io.Serializable;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.MGRSUtils;

public class LocalizedSample implements Serializable {
    Sample basicSample;
    String mgrsCoords;
    String timestamp;

    public LocalizedSample(Sample sample, String mgrsCoords, String timestamp) {
        this.basicSample = sample;
        this.mgrsCoords = mgrsCoords;
        this.timestamp = timestamp;
    }

    public Sample getBasicSample() {
        return basicSample;
    }

    public void setBasicSample(Sample basicSample) {
        this.basicSample = basicSample;
    }

    public String getMgrsCoords() {
        return mgrsCoords;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setMgrsCoords(String mgrsCoords) {
        this.mgrsCoords = mgrsCoords;
    }
}
