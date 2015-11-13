package com.innav.innav.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by Seif3 on 10/29/2015.
 */
public class VenueProvider extends ContentProvider
{
    static final int VENUES = 100;
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sQueryBuilder;

    static
    {
        sQueryBuilder = new SQLiteQueryBuilder();
        sQueryBuilder.setTables(VenueDBContract.VenuesEntry.TABLE_NAME);
    }

    private VenueDBHelper mOpenHelper;

    static UriMatcher buildUriMatcher()
    {
        // 1) The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case. Add the constructor below.
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = VenueDBContract.CONTENT_AUTHORITY;

        // 2) Use the addURI function to match each of the types.  Use the constants from
        // WeatherContract to help define the types to the UriMatcher.
        matcher.addURI(authority, VenueDBContract.PATH_VENUES, VENUES);
//        matcher.addURI(authority, VenueDBContract.PATH_BEACONS + "/*", WEATHER_WITH_LOCATION);
//        matcher.addURI(authority, VenueDBContract.PATH_PLACES + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
//        matcher.addURI(authority, VenueDBContract.PATH_WIFI_APS, LOCATION);

        // 3) Return the new matcher!
        return matcher;
    }

    @Override
    public boolean onCreate()
    {
        mOpenHelper = new VenueDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        return sQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );

    }

    @Nullable
    @Override
    public String getType(Uri uri)
    {
        return VenueDBContract.VenuesEntry.CONTENT_TYPE;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        Uri returnUri;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long _id = db.insert(VenueDBContract.VenuesEntry.TABLE_NAME,
                null,
                values);
        if (_id > 0)
        {
            returnUri = VenueDBContract.VenuesEntry.buildVenuesUri(_id);
        }
        else
        {
            throw new android.database.SQLException("Failed to insert row into " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsDeleted;
        rowsDeleted = db.delete(VenueDBContract.VenuesEntry.TABLE_NAME, selection, selectionArgs);

        if (rowsDeleted != 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rowsImpacted;

        rowsImpacted = db.update(VenueDBContract.VenuesEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsImpacted != 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsImpacted;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        int returnCount = 0;
        try
        {
            for (ContentValues value : values)
            {
                long _id = db.insert(VenueDBContract.VenuesEntry.TABLE_NAME, null, value);
                if (_id != -1)
                {
                    returnCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally
        {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }
}
