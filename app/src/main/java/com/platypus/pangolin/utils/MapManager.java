package com.platypus.pangolin.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.platypus.pangolin.activities.TileInfoActivity;
import com.platypus.pangolin.models.SampleType;

import java.util.ArrayList;
import java.util.List;

import mil.nga.mgrs.MGRS;
import mil.nga.mgrs.grid.GridType;
import mil.nga.mgrs.tile.MGRSTileProvider;

public class MapManager {
    private GoogleMap map;
    private List<Polygon> polygonList;
    private List<TileOverlay> tilesSchemas;
    private MGRSTileProvider tileProvider;
    private Context context;
    private int zoom;

    public MapManager(GoogleMap map, Context context) {
        this.map = map;
        this.polygonList = new ArrayList<>();
        this.tilesSchemas = new ArrayList<>();
        this.context = context;

        map.setOnPolygonClickListener(polygon -> {
            // Perform the desired action when the polygon is clicked
            // For example, show an info window or change the polygon color
            String data = (String) polygon.getTag();
            Intent i = new Intent(context, TileInfoActivity.class);
            i.putExtra("Data", data);
            context.startActivity(i);
            System.out.println(data);
        });
    }

    public void moveCameraTo(LatLng coords){
        map.moveCamera(CameraUpdateFactory.newLatLng(coords));
    }

    public void drawGrid(GridType gridType){
        removeGrid();
        tileProvider = MGRSTileProvider.create(context, GridType.GZD, gridType);
        TileOverlay tl = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        tilesSchemas.add(tl);

        //se la precisione è 10 metri
        if (gridType.getPrecision() == 10)
            zoom = 19;
        else if (gridType.getPrecision() == 100)
            zoom = 16;
        else
            zoom = 13;

        map.moveCamera(CameraUpdateFactory.zoomTo(zoom));
    }

    public void removeGrid(){
        for (TileOverlay t : tilesSchemas){
            t.remove();
        }
        tilesSchemas.clear();
    }

    @SuppressLint("MissingPermission")
    public void setLocationEnabled(){
        map.setMyLocationEnabled(true);
    }

    @SuppressLint("MissingPermission")
    public void moveCameraToUserPosition(FusedLocationProviderClient flocation){
        flocation
                .getLastLocation()
                .addOnSuccessListener((Activity) context, location -> {
                    if (location != null) {
                        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraPosition cp = new CameraPosition.Builder()
                                .target(pos)
                                .zoom(zoom)
                                .build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
                    }
                });
    }

    public void colorTile(MGRS coords, int color, GridType gridType, SampleType sampleType) {
        if (map == null)
            return;

        PolygonOptions currentTile = new PolygonOptions().addAll(MGRSTools.calculateTileCorners(coords, gridType));
        Polygon p = map.addPolygon(currentTile);
        p.setStrokeWidth(0);
        p.setFillColor(color);
        p.setClickable(true);

        String tag = coords.toString() + "_" + sampleType.toString() + "_" + gridType.getAccuracy();
        p.setTag(tag);

        polygonList.add(p);
    }

    public void deleteAllPolygons(){
        for (Polygon p : polygonList){
            p.remove();
        }
        polygonList.clear();
    }

    private int getColorByValue(int val) {
        if (val == 0)
            return Color.rgb(255,0,0);
        else if (val == 1)
            return Color.rgb(255,255,0);
        else
            return Color.rgb(0, 255, 0);
    }

    public MGRS getMGRS(LatLng latlng){
        return tileProvider.getMGRS(latlng);
    }

}
