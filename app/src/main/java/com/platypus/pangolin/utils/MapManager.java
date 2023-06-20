package com.platypus.pangolin.utils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.ArrayList;
import java.util.List;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;

public class MapManager {
    private GoogleMap map;
    private List<Polygon> polygonList;
    private List<TileOverlay> tilesSchemas;


    public MapManager(GoogleMap map) {
        this.map = map;
        polygonList = new ArrayList<>();
        tilesSchemas = new ArrayList<>();
    }

    public void colorTile(MGRS coords, int color, GridType gridType) {
        if (map == null)
            return;

        PolygonOptions currentTile = new PolygonOptions().addAll(MGRSTools.calculateTileCorners(coords, gridType));
        Polygon p = map.addPolygon(currentTile);
        p.setStrokeWidth(0);
        p.setFillColor(color);

        polygonList.add(p);
    }

    public void deleteAllTiles(){
        for (Polygon p : polygonList){
            p.remove();
        }
        polygonList.clear();
    }





}
