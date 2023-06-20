package com.platypus.pangolin.models;

import com.google.android.gms.maps.model.LatLng;
import com.platypus.pangolin.utils.MGRSTools;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.MGRSUtils;

public class LocalizedSample {
    Sample basicSample;
    String mgrsCoords;

    public LocalizedSample(Sample sample, String mgrsCoords) {
        this.basicSample = sample;
        this.mgrsCoords = mgrsCoords;
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

    public void setMgrsCoords(String mgrsCoords) {
        this.mgrsCoords = mgrsCoords;
    }
}
