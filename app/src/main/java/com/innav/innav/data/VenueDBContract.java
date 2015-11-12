package com.innav.innav.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Seif3 on 10/29/2015.
 */
public class VenueDBContract
{
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.innav.innav";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_VENUES = "venues";
    public static final String PATH_WIFI_APS = "wifi";
    public static final String PATH_BEACONS = "beacons";
    public static final String PATH_PLACES = "places";

    /*
        Inner class that defines the table contents of the Nearby Venues table
     */
    public static final class VenuesEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "venues";

        public static final String COLUMN_NAME = "v_name";
        public static final String COLUMN_ID = "v_id";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_VENUES).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_VENUES;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_VENUES;

        public static Uri buildVenuesUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

    }

    public static final class BeaconsEntry implements BaseColumns
    {

    }


}
