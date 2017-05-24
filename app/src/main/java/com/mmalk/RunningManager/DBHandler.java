package com.mmalk.RunningManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Class used to handle operations considering database
 * the database contains 4 columns, each for different field of Result object
 */
public class DBHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "resultsDB.db";
    private static final String TABLE_RESULTS = "results";
    private static final String COLUMN_DISTANCE = "_distance";
    private static final String COLUMN_TIME = "_time";
    private static final String COLUMN_DATE = "_date";
    private static final String COLUMN_COMMENT = "_comment";

    private static final String TAG = "DBHandler";

    public DBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    /**
     * creation of the database
     * COLUMN_DATE is used as a primary key
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_RESULTS + " (" +
                COLUMN_DISTANCE + " INTEGER, " +
                COLUMN_TIME + " INTEGER, " +
                COLUMN_DATE + " INTEGER PRIMARY KEY, " +
                COLUMN_COMMENT + " TEXT);";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESULTS);
        onCreate(db);
    }

    /**
     * method used to add result to the database
     * it takes Result object as a parameter
     */
    public void addResult(Result result) {

        ContentValues values = new ContentValues();
        values.put(COLUMN_DISTANCE, result.getDistance());
        values.put(COLUMN_TIME, result.getTime());
        values.put(COLUMN_DATE, result.getDate());
        values.put(COLUMN_COMMENT, result.getComment());
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_RESULTS, null, values);
        db.close();
        Log.i(TAG, "Saving date: " + result.getDate());
    }

    /**
     * method used to delete entry from the database
     * the deletion is done according to the date of Result, which is unique key in database
     */
    public void deleteResult(long date) {

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_RESULTS + " WHERE " + COLUMN_DATE + "=\"" + date + "\";");
    }

    /**
     * method used to read all entries from the database
     * it returns an array of Result objects
     */
    public Result[] databaseToResultArray() {

        Result[] res;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_RESULTS + " WHERE 1";

        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();

        Log.i("", "getCount = " + cursor.getCount());
        Log.i("", "getColumnCount = " + cursor.getColumnCount());
        res = new Result[cursor.getCount()];

        int counter = 0;

        while (!cursor.isAfterLast()) {
            if (cursor.getString(cursor.getColumnIndex("_date")) != null) {

                res[counter] = new Result();
                res[counter].setDistance(cursor.getInt(cursor.getColumnIndex("_distance")));
                res[counter].setTime(cursor.getInt(cursor.getColumnIndex("_time")));
                res[counter].setDate(cursor.getLong(cursor.getColumnIndex("_date")));
                res[counter].setComment(cursor.getString(cursor.getColumnIndex("_comment")));
                counter++;
            }
            cursor.moveToNext();
        }

        db.close();
        return res;
    }
}
