package com.platypus.pangolin.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.platypus.pangolin.models.SampleType;

public class DatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String DATABASE_NAME = "Pangolin.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "Samples";

    private static final String COLUMN_DATA = "date";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_CONDITION = "condition";
    private static final String COLUMN_COORDINATE = "coordinate";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

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

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void addSample(String type, String data, double value, int condition, String coordinate){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_TYPE, type);
        cv.put(COLUMN_DATA, data);
        cv.put(COLUMN_VALUE, value);
        cv.put(COLUMN_CONDITION, condition);
        cv.put(COLUMN_COORDINATE, coordinate);

        long result = db.insert(TABLE_NAME, null, cv);

        if (result == -1)
            Toast.makeText(context, "Errore durante la scrittura del DB", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, "Inserimento avvenuto correttamente", Toast.LENGTH_SHORT).show();
    }

    public Cursor getSamplesByType(SampleType type){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT date, value, condition, coordinate FROM " + TABLE_NAME + " WHERE type = '" + type.toString() + "'";
        return db.rawQuery(query, null);
    }

    public Cursor getValueByAccuracyAndDate(int accuracy, int dateLimit){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT zone, " +
                "band," +
                "substr(easting, 1, 3) AS east_cast," +
                "substr(northing, 1, 3) AS north_cast," +
                "AVG(cond) AS media_cond" +
                "FROM (" +
                "    SELECT data, tipo, val, cond, zone, band, easting, northing, " +
                "           ROW_NUMBER() OVER (PARTITION BY substr(easting, 1, 3), substr(northing, 1, 3) ORDER BY data) AS rn\n" +
                "    FROM sample" +
                ")" +
                "WHERE rn <= " + accuracy + " " +
                "GROUP BY zone, band, east_cast, north_cast";
        return null;
    }

    public void resetDB(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);

    }
}
