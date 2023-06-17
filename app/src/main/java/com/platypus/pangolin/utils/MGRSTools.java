package com.platypus.pangolin.utils;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;

public class MGRSTools {

    /*
    Converte una coordinata MGRS in latitudine e longutine
     */
    public static LatLng fromMGRStoLatLng(MGRS mgrsCoord){
        Point p = mgrsCoord.toPoint();
        return new LatLng(p.getLatitude(), p.getLongitude());
    }

    /*
    Questo metodo prende in input una coordinata espressa in MGRS
    e il tipo di griglia con cui si lavora, e ritorna una lista di
    coordinate (Latitudine, Longitudine) dei vertici del quadrato
     */
    public static List<LatLng> calculateTileCorners(MGRS coord, GridType gridType) {
        int precision = gridType.getPrecision();
        List<LatLng> corners = new ArrayList<>();

        int zone = coord.getZone();
        char band = coord.getBand();
        char column = coord.getColumn();
        char row = coord.getRow();


        long xbs = (coord.getEasting()/precision) * precision;
        long ybs = (coord.getNorthing()/precision) * precision;

        long xbd = xbs+ precision;
        long ybd = ybs;

        long xas = xbs;
        long yas = ybs + precision;

        long xad = xbs + precision;
        long yad = ybs + precision;

        MGRS mgrsBS = new MGRS(zone, band, column, row, xbs, ybs);
        MGRS mgrsBD = new MGRS(zone, band, column, row,  xbd, ybd);
        MGRS mgrsAS = new MGRS(zone, band, column, row,  xas, yas);
        MGRS mgrsAD = new MGRS(zone, band, column, row,  xad, yad);

        corners.add(fromMGRStoLatLng(mgrsBS));
        corners.add(fromMGRStoLatLng(mgrsBD));
        corners.add(fromMGRStoLatLng(mgrsAD));
        corners.add(fromMGRStoLatLng(mgrsAS));

        return corners;
    }
}
