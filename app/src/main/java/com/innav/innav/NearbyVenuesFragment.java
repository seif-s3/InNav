package com.innav.innav;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class NearbyVenuesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    static final int COL_VENUE_ID = 0;
    static final int COL_VENUE_NAME = 1;
    private static final int VENUES_LOADER = 0;
    private static final String[] VENUES_COLUMNS = {
            VenueDBContract.VenuesEntry.TABLE_NAME + "." + VenueDBContract.VenuesEntry._ID,
            VenueDBContract.VenuesEntry.COLUMN_NAME,
            VenueDBContract.VenuesEntry.COLUMN_ID
    };
    private ArrayAdapter<String> mNearbyVenuesAdapter;
    public NearbyVenuesFragment()
    {
    }
    @Override
    public void onStart()
    {
        super.onStart();
        getVenueNames();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_nearby_venues, container, false);
        mNearbyVenuesAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_nearby_venue,
                R.id.nearby_venue_name_tv,
                new ArrayList<String>());

        final ListView listView = (ListView) rootView.findViewById(R.id.list_view_nearby);
        listView.setAdapter(mNearbyVenuesAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String venue = mNearbyVenuesAdapter.getItem(position);
                Intent launchMapActivity = new Intent(getActivity(), MapWebView.class);
                launchMapActivity.putExtra("place_name", venue);
                startActivity(launchMapActivity);
                //Toast t = Toast.makeText(getContext(), "Map coming soon..", Toast.LENGTH_LONG);
                //t.show();
            }
        });

        return rootView;
    }

    private void getVenueNames()
    {
        FetchNearbyVenuesTask f = new FetchNearbyVenuesTask();
        // TODO: Send GPS coordinates instead of dummy value
        f.execute("30", "31", "10");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {

    }

    public class FetchNearbyVenuesTask extends AsyncTask<String, Void, String[]>
    {
        private final String LOG_TAG = FetchNearbyVenuesTask.class.getSimpleName();

        @Override
        protected void onPostExecute(String[] result)
        {
            if (result != null)
            {
                getView().findViewById(R.id.nearby_venues_text).setVisibility(View.GONE);
                mNearbyVenuesAdapter.clear();
                for (String venue : result)
                {
                    mNearbyVenuesAdapter.add(venue);
                }
            }
            else
            {
                if (result.length == 0)
                {
                    ((TextView) getView().findViewById(R.id.nearby_venues_text)).setText("No Venues Found.");
                }
                else
                {
                    getView().findViewById(R.id.nearby_venues_text).setVisibility(View.VISIBLE);
                }
            }

        }

        @Override
        protected String[] doInBackground(String... params)
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
                final String BASE_URL = "https://mazein.herokuapp.com/places.json?";

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
                return getNearbyVenuesFromJson(nearbyVenuesJsonStr);
            } catch (JSONException e)
            {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        private String[] getNearbyVenuesFromJson(String jsonStr) throws JSONException
        {
            final String VENUE_NAME_KEY = "name";
            //JSONObject resultJson = new JSONObject(jsonStr);
            JSONArray venuesArray = new JSONArray(jsonStr);
            String[] venueNames = new String[venuesArray.length()];

            //Log.v("JSONResult", Integer.toString(jsonArray.length()));
            for (int i = 0; i < venuesArray.length(); i++)
            {
                JSONObject venueDetails = venuesArray.getJSONObject(i);
                venueNames[i] = venueDetails.getString(VENUE_NAME_KEY);
            }
            return venueNames;

        }
    }
}
