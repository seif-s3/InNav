package com.innav.innav;

import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;

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
public class MainActivityFragment extends Fragment
{
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;

    private ArrayList<MainMenuItem> navDrawerItems;
    private MainMenuListAdapter adapter;

    private Integer numberOfNearbyVenuesIndicator = new Integer(0);

    public MainActivityFragment()
    {
    }

    @Override
    public void onStart()
    {
        super.onStart();
        getNumberOfNearbyVenues();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_refresh)
        {
            getNumberOfNearbyVenues();
            return true;
        }
        else if (id == R.id.action_settings)
        {
            Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final ListView listView = (ListView)  rootView.findViewById(R.id.main_menu_list_view);

        navMenuIcons = getResources().obtainTypedArray(R.array.main_menu_icons);
        navMenuTitles = getResources().getStringArray(R.array.main_menu_items);

        navDrawerItems = new ArrayList<>();
        // TODO: Move main menu creation to onStart method.
        // TODO: Create helper function for creating the main menu
        // TODO: Get number of Nearby Venues from server and add them dynamically here..

        getNumberOfNearbyVenues();

        navDrawerItems.add(new MainMenuItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1), true, Integer.toString(numberOfNearbyVenuesIndicator.intValue())));
        navDrawerItems.add(new MainMenuItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1) ));
        navDrawerItems.add(new MainMenuItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1) ));
        navDrawerItems.add(new MainMenuItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));

        //navMenuIcons.recycle();
        adapter = new MainMenuListAdapter(getContext(), navDrawerItems);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent launchSelectedActivity = null;
                if (position == 0)
                {
                    launchSelectedActivity = new Intent(getActivity(), NearbyVenues.class);
                }
                else if (position == 1)
                {
                    launchSelectedActivity = new Intent(getActivity(), CompassActivity.class);
                }
                else if (position == 3)
                {
                    launchSelectedActivity = new Intent(getActivity(), SettingsActivity.class);
                }
                try
                {
                    startActivity(launchSelectedActivity);
                } catch (Exception e)
                {
                    Log.e("MainMenuActivity", "Could not start Activity");
                }
            }
        });
        return rootView;
    }

    private void getNumberOfNearbyVenues()
    {
        FetchNearbyVenuesTask f = new FetchNearbyVenuesTask();
        // TODO: Read lat and long from GPS and range from SharedPref
        f.execute("30", "31", "5");
    }

    public class FetchNearbyVenuesTask extends AsyncTask<String, Void, Integer>
    {
        private final String LOG_TAG = FetchNearbyVenuesTask.class.getSimpleName();

        @Override
        protected void onPostExecute(Integer result)
        {
            if (result != null)
            {
                navDrawerItems.clear();
                navDrawerItems.add(new MainMenuItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1), true, Integer.toString(numberOfNearbyVenuesIndicator.intValue())));
                navDrawerItems.add(new MainMenuItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
                navDrawerItems.add(new MainMenuItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
                navDrawerItems.add(new MainMenuItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));
            }
        }

        @Override
        protected Integer doInBackground(String... params)
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
                    return 0;
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

        private Integer getNearbyVenuesFromJson(String jsonStr) throws JSONException
        {

            //JSONObject resultJson = new JSONObject(jsonStr);
            JSONArray jsonArray = new JSONArray(jsonStr);
            Log.v("JSONResult", Integer.toString(jsonArray.length()));
            return new Integer(jsonArray.length());

        }
    }
}
