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

        //coordinate vertice basso sinistra
        long xbs = (coord.getEasting()/precision) * precision;
        long ybs = (coord.getNorthing()/precision) * precision;

        //coordinate vertice basso destra
        long xbd = xbs + precision;
        long ybd = ybs;

        //coordinate vertice alto sinistra
        long xas = xbs;
        long yas = ybs + precision;

        //coordinate vertice alto destra
        long xad = xbs + precision;
        long yad = ybs + precision;

        //trasformo i vertici in coordinate MGRS
        MGRS mgrsBS = new MGRS(zone, band, column, row, xbs, ybs);
        MGRS mgrsBD = new MGRS(zone, band, column, row,  xbd, ybd);
        MGRS mgrsAS = new MGRS(zone, band, column, row,  xas, yas);
        MGRS mgrsAD = new MGRS(zone, band, column, row,  xad, yad);

        //trasforo le coordinate in LatLng e le aggiungo alla lista
        corners.add(fromMGRStoLatLng(mgrsBS));
        corners.add(fromMGRStoLatLng(mgrsBD));
        corners.add(fromMGRStoLatLng(mgrsAD));
        corners.add(fromMGRStoLatLng(mgrsAS));

        return corners;
    }

    public static MGRS fromStringToMGRS(String mgrsString) {

        int offset = 0;

        if (mgrsString.length() % 2 != 0){
            offset = 1;
        }

        int zone = Integer.parseInt(mgrsString.substring(0, offset + 1));
        char band = mgrsString.charAt(1 + offset);
        char column = mgrsString.charAt(2 + offset);
        char row = mgrsString.charAt(3 + offset);

        String ne = mgrsString.substring(4 + offset);
        long e = Integer.parseInt(ne.substring(0, ne.length()/2));
        long n = Integer.parseInt(ne.substring(ne.length()/2));

        return new MGRS(zone, band, column, row, e, n);
    }

    public static MGRS fromStringToMGRS(String mgrsString, int accuracy){
        //l'offset è 1 se la stringa ha lunghezza dispari, significa che la
        //gridzone è composta da due cifre e il numero
        int offset = mgrsString.length() % 2 != 0 ? 1 : 0;

        int zone = Integer.parseInt(mgrsString.substring(0, offset + 1));
        char band = mgrsString.charAt(1 + offset);
        char column = mgrsString.charAt(2 + offset);
        char row = mgrsString.charAt(3 + offset);

        String ne = mgrsString.substring(4 + offset);
        //i math.pow sono necessari per creare la coordinata corretta,
        //senza gli easting e northing vengono letti come un long senza gli zeri davanti
        long e = (int) (Integer.parseInt(ne.substring(0, accuracy)) * Math.pow(10, 5 - accuracy));
        long n = (int) (Integer.parseInt(ne.substring(ne.length()/2,  ne.length()/2 + accuracy)) * Math.pow(10, 5 -accuracy));

        return new MGRS(zone, band, column, row, e, n);
    }

    public static String castMGRSCoord(String mgrsString, int accuracy){
        int offset = 0;

        if (mgrsString.length() % 2 != 0){
            offset = 1;
        }

        int zone = Integer.parseInt(mgrsString.substring(0, offset + 1));
        char band = mgrsString.charAt(1 + offset);
        char column = mgrsString.charAt(2 + offset);
        char row = mgrsString.charAt(3 + offset);

        String ne = mgrsString.substring(4 + offset);
        String e = ne.substring(0, accuracy);
        String n = ne.substring(ne.length()/2,  ne.length()/2 + accuracy);
        return zone + "" + band + column + row + e + n ;
    }
}
