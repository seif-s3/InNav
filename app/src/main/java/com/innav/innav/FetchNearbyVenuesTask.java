package com.innav.innav;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.innav.innav.data.VenueDBContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;


public class FetchNearbyVenuesTask extends AsyncTask<String, Void, Void>
{
    private final String LOG_TAG = FetchNearbyVenuesTask.class.getSimpleName();
    private final Context mContext;

    public FetchNearbyVenuesTask(Context context)
    {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params)
    {
        final String LOG_TAG = "ServerConnection";
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String nearbyVenuesJsonStr = null;


        try
        {
            final String BASE_URL = "https://limitless-depths-3645.herokuapp.com/places.json?";

            final String LAT_PARAM = "lat";
            final String LONG_PARAM = "long";
            final String RANGE_PARAM = "range";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(LAT_PARAM, params[0])
                    .appendQueryParameter(LONG_PARAM, params[1])
                    .appendQueryParameter(RANGE_PARAM, params[2])
                    .build();
            URL url = new URL(builtUri.toString());
            Log.v(LOG_TAG, "Built URL: " + url);
            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null)
            {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null)
            {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0)
            {
                // Stream was empty.  No point in parsing.
                return null;
            }
            nearbyVenuesJsonStr = buffer.toString();
            Log.v(LOG_TAG, nearbyVenuesJsonStr);
        } catch (IOException e)
        {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            //Toast.makeText(mContext, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return null;
        } finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (final IOException e)
                {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        try
        {
            getNearbyVenuesFromJson(nearbyVenuesJsonStr);
            return null;
        } catch (JSONException e)
        {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    private void getNearbyVenuesFromJson(String jsonStr) throws JSONException
    {
        final String VENUE_NAME_KEY = "name";
        final String VENUE_ID_KEY = "id";
        //JSONObject resultJson = new JSONObject(jsonStr);
        JSONArray venuesArray = new JSONArray(jsonStr);
        //String[] venueNames = new String[venuesArray.length()];

        Vector<ContentValues> cVVector = new Vector<>(venuesArray.length());
        //Log.v("JSONResult", Integer.toString(jsonArray.length()));
        try
        {
            for (int i = 0; i < venuesArray.length(); i++)
            {
                ContentValues venueName = new ContentValues();
                JSONObject venueDetails = venuesArray.getJSONObject(i);
                venueName.put(VenueDBContract.VenuesEntry.COLUMN_ID, venueDetails.getString(VENUE_ID_KEY));
                venueName.put(VenueDBContract.VenuesEntry.COLUMN_NAME, venueDetails.getString(VENUE_NAME_KEY));
                cVVector.add(venueName);
            }
            int inserted = 0;
            if (cVVector.size() > 0)
            {
                ContentValues[] cVArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cVArray);
                inserted = mContext.getContentResolver().bulkInsert(VenueDBContract.VenuesEntry.CONTENT_URI, cVArray);
                Log.d(LOG_TAG, "FetchNearbyVenuesTask completed " + inserted + " Inserted");
            }
            return;
        } catch (JSONException e)
        {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }


}
