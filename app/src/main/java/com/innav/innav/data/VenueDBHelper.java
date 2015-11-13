package com.innav.innav.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.innav.innav.data.VenueDBContract.VenuesEntry;

/**
 * Created by Seif3 on 10/29/2015.
 */
public class VenueDBHelper extends SQLiteOpenHelper
{
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "venues.db";

    public VenueDBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // Create a table to hold locations.  A location consists of the string supplied in the
        // location setting, the city name, and the latitude and longitude
        final String SQL_CREATE_VENUES_TABLE = "CREATE TABLE " + VenuesEntry.TABLE_NAME + " (" +
                VenuesEntry._ID + " INTEGER PRIMARY KEY," +
                VenuesEntry.COLUMN_ID + " TEXT UNIQUE NOT NULL, " +
                VenuesEntry.COLUMN_NAME + " TEXT NOT NULL " +
                " );";

        db.execSQL(SQL_CREATE_VENUES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next line
        // should be your top priority before modifying this method.

        db.execSQL("DROP TABLE IF EXISTS " + VenuesEntry.TABLE_NAME);
        onCreate(db);
    }
}
