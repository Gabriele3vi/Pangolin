package com.platypus.pangolin.models;

import com.google.android.gms.maps.model.LatLng;
import com.platypus.pangolin.utils.MGRSTools;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.MGRSUtils;

public class LocalizedSample {
    Sample basicSample;
    LatLng latLngCoords;
    MGRS mgrsCoords;

    public LocalizedSample(Sample sample, LatLng coords, MGRS mgrsCoords) {
        this.basicSample = sample;
        this.latLngCoords = coords;
        this.mgrsCoords = mgrsCoords;
    }

    public Sample getBasicSample() {
        return basicSample;
    }

    public void setBasicSample(Sample basicSample) {
        this.basicSample = basicSample;
    }

    public LatLng getLatLngCoords() {
        return latLngCoords;
    }

    public void setLatLngCoords(LatLng latLngCoords) {
        this.latLngCoords = latLngCoords;
    }

    public MGRS getMgrsCoords() {
        return mgrsCoords;
    }

    public void setMgrsCoords(MGRS mgrsCoords) {
        this.mgrsCoords = mgrsCoords;
    }
}
