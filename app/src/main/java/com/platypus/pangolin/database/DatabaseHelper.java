package com.platypus.pangolin.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.platypus.pangolin.models.SampleType;

import java.sql.Timestamp;

public class DatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "Pangolin.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "Samples";

    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_CONDITION = "condition";
    //private static final String COLUMN_COORDINATE = "coordinate";
    private static final String COLUMN_GRIDZONE = "gridzone";
    private static final String COLUMN_SQUARE = "square";
    private static final String COLUMN_EASTING = "easting";
    private static final String COLUMN_NORTHING = "northing";


    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    /*
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " +  TABLE_NAME +
                " ("  +  COLUMN_TYPE +  " VARCHAR(30) NOT NULL," +
                COLUMN_DATA + " TIMESTAMP NOT NULL," +
                COLUMN_VALUE + " REAL NOT NULL," +
                COLUMN_CONDITION + " INTEGER NOT NULL," +
                COLUMN_COORDINATE +  " VARCHAR(15) NOT NULL," +
                " primary key("+ COLUMN_DATA + "," + COLUMN_TYPE + ")" +
                ");";

        db.execSQL(query);

    }*/

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " +  TABLE_NAME +
                " ("  +  COLUMN_TYPE +  " VARCHAR(30) NOT NULL," +
                COLUMN_TIMESTAMP + " TIMESTAMP NOT NULL," +
                COLUMN_VALUE + " REAL NOT NULL," +
                COLUMN_CONDITION + " INTEGER NOT NULL," +
                COLUMN_GRIDZONE +  " VARCHAR(3) NOT NULL," +
                COLUMN_SQUARE +  " VARCHAR(2) NOT NULL," +
                COLUMN_EASTING +  " VARCHAR(5) NOT NULL," +
                COLUMN_NORTHING +  " VARCHAR(5) NOT NULL," +
                " primary key("+ COLUMN_TIMESTAMP + "," + COLUMN_TYPE + ")" +
                ");";

        db.execSQL(query);

    }

    public void addSample(
            String type,
            String timeStamp,
            double value,
            int condition,
            String gridzone,
            String square,
            String easting,
            String northing)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_TYPE, type);
        cv.put(COLUMN_TIMESTAMP, timeStamp);
        cv.put(COLUMN_VALUE, value);
        cv.put(COLUMN_CONDITION, condition);
        cv.put(COLUMN_GRIDZONE, gridzone);
        cv.put(COLUMN_SQUARE, square);
        cv.put(COLUMN_EASTING, easting);
        cv.put(COLUMN_NORTHING, northing);

        long result = db.insert(TABLE_NAME, null, cv);

        if (result == -1)
            Toast.makeText(context, "Errore durante la scrittura del DB", Toast.LENGTH_LONG).show();
    }




    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public Cursor getSamplesByType(String type){
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "select * " +
                "from Samples " +
                "where type = '" + type + "' " +
                " order by timestamp desc";

        return db.rawQuery(query, null);

    }

    public Cursor getMissingSampleTypes
            (int accuracy,
             String gridzoneInput,
             String squareInput,
             String eastingInput,
             String northingInput)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT type_enum " +
                "FROM (SELECT 'Signal' AS type_enum " +
                "UNION " +
                "SELECT 'Wifi' " +
                "UNION " +
                "SELECT 'Noise') AS type_list " +
                "WHERE type_enum NOT IN (" +
                "SELECT  DISTINCT type " +
                "from Samples " +
                "where date(timestamp) =  date('now') " +
                "and  gridzone = '" +  gridzoneInput + "' "+
                "and square = '" + squareInput  +  "' " +
                "and substr(easting, 1," + accuracy + ") = substr('" +  eastingInput + "'"+", 1," + accuracy + ") " +
                "and  substr(northing, 1," + accuracy + ") = substr('" +  northingInput + "'"+", 1, " + accuracy + "));";
        return db.rawQuery(query, null);
    }


    public Cursor getSamplesByType(SampleType type){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT date, value, condition, coordinate FROM " + TABLE_NAME + " WHERE type = '" + type.toString() + "'";
        return db.rawQuery(query, null);
    }

public Cursor getSamplesByCoordAndAccuracyAndType
          (String gridzoneInput,
           String squareInput,
           String eastingInput,
           String northingInput,
           String type,
           String accuracy)
        {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT timestamp, value, condition " +
                "FROM Samples " +
                "where type = ? " +
                "and  gridzone = ? " +
                "and square = ? "+
                "and substr(easting, 1, ?) = substr(?, 1, ?) " +
                "and  substr(northing, 1, ?) = substr(?, 1, ?) " +
                "order by timestamp;";
        String [] args =
                {type, gridzoneInput, squareInput, accuracy, eastingInput, accuracy, accuracy, northingInput, accuracy};
        return db.rawQuery(query, args);
    }

    /*
    * QUERY DI RIFERIMENTO, STUDIALA IN VISTA DELL'ESAME
        SELECT  zone,
                band,
                substr(easting, 1, 3) AS east_cast,
                substr(northing, 1, 3) AS north_cast,
                AVG(cond) AS media_cond
        FROM (
            SELECT data, tipo, val, cond, zone, band, easting, northing,
            ROW_NUMBER() OVER (PARTITION BY substr(easting, 1, 3), substr(northing, 1, 3) ORDER BY data) AS rn
            FROM sample
        ) AS subquery
        WHERE rn <= 5 and tipo = "noise"
        GROUP BY  zone, band, east_cast, north_cast
        *
        * */

    //TODO pulisci questo codice che è illeggibile, usa il metodo corretto
    public Cursor getAvgConditionByAccuracy(String sampleType, int accuracy, int dateLimit){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_GRIDZONE + ", "
                + COLUMN_SQUARE +"," +
                "substr(" + COLUMN_EASTING + ", 1," +  accuracy + ") AS easting_cast," +
                "substr(" + COLUMN_NORTHING + ", 1," +  accuracy + ") AS northing_cast," +
                "AVG(" + COLUMN_CONDITION+ ") AS avg_cond " +
                "FROM (" +
                "    SELECT timestamp, type, value, condition, gridzone, square, easting, northing, " +
                "           ROW_NUMBER() OVER (PARTITION BY substr(easting, 1," +  accuracy + "), substr(northing, 1," +  accuracy +
                ") ORDER BY timestamp) AS rn " +
                "    FROM samples" +
                ") AS subquery " +
                "WHERE rn <= " + dateLimit + " AND type = '" + sampleType+ "' " +
                "GROUP BY gridzone, square, easting_cast, northing_cast";
        return db.rawQuery(query, null);
    }

    public void resetDB(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public int deleteSample (String timestamp, String type){
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "timestamp = ? and type = ?";
        String [] whereArgs = {timestamp, type};
        return db.delete(TABLE_NAME, whereClause, whereArgs);
    }
}
