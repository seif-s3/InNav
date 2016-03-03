package com.innav.innav;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Seif3 on 1/23/2016.
 */
public class MapWebView extends AppCompatActivity
{
    private static final String LOG_TAG = "MAP_WEBVIEW";

    private WebView mapWebView;
    private ProgressDialog mProgressDialog;
    private WifiManager mWifiManager;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mBroadcastReceiver;
    private ArrayList<ScanResult> mScanResults;
    private ArrayList<JSONObject> wifiFingerprints = new ArrayList<JSONObject>();

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mapWebView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        mapWebView.restoreState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String place_id = getIntent().getStringExtra("place_name");
        Log.i("MAPVIEW_INTENT", place_id);
        setContentView(R.layout.map_webview);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mapWebView = (WebView) findViewById(R.id.webView);
        WebSettings mapWebSettings = mapWebView.getSettings();
        mapWebSettings.setJavaScriptEnabled(true);
        mapWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        mapWebView.loadUrl("file:///android_asset/floor_plans/" + place_id + ".html");
    }

    private String makePostRequest(JSONObject finger_print) throws IOException
    {
        try
        {
            //final JSONobject which would be serialized to be sent to the server including wifiFingerprints JSONObject
            JSONObject requestJson = new JSONObject();
            requestJson.put("action", "localization");
            requestJson.put("controller", "finger_prints");
            requestJson.put("finger_print", finger_print);

            HttpURLConnection con = (HttpURLConnection) (new URL("https://mazein.herokuapp.com/localization.json").openConnection());
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("POST");

            Log.d("JSON_TO_SERVER", finger_print.toString());
            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(requestJson.toString());
            Log.d("RequestJson", requestJson.toString());
            wr.flush();
            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK)
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null)
                {
                    sb.append(line + "\n");
                }

                br.close();
                Log.i("JSON_TO_SERVER", "Response: " + sb.toString());
                return sb.toString();
            }
            else
            {
                System.out.println(con.getResponseMessage());
//                Toast.makeText(Fingerprinting.this, "Server Response: ERR", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e)
        {
            Log.e("POST_REQ", "Couldn't convert Fingerprint to JSON");
            e.printStackTrace();
        }
        return null;

    }

    private String makeGetRequest(JSONObject finger_print) throws IOException
    {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String currentCoordinates = null;

        try
        {
            final String BASE_URL = "https://mazein.herokuapp.com/localization.json?";
            /// /final JSONobject which would be serialized to be sent to the server including wifiFingerprints JSONObject

            final String FP_PARAM = "wifiFingerprints";
            JSONObject fingerprintJson = new JSONObject();
            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(FP_PARAM, "\"" + finger_print.toString() + "\"")
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
            currentCoordinates = buffer.toString();
            Log.v(LOG_TAG, currentCoordinates);
            return currentCoordinates;
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
    }

    public class WebAppInterface
    {
        Context mContext;
        /**
         * Start the WIFI scan
         */
        private Runnable scanWifi = new Runnable()
        {

            public void run()
            {
                mWifiManager.startScan();
            }
        };

        WebAppInterface(Context c)
        {
            mContext = c;
        }

        @JavascriptInterface
        public void initializeWifiScan(final String place_id, final float startX, final float startY)
        {
            int scanNumber = 1;
            //mProgressDialog = ProgressDialog.show(MapWebView.this, "WiFi Scan",
            //            "Scan " + String.valueOf(scanNumber) + " at: \n" + String.valueOf(startX) + ", " + String.valueOf(startY), true);

            mIntentFilter = new IntentFilter();

            mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            mBroadcastReceiver = new BroadcastReceiver()
            {

                @Override
                public void onReceive(Context context, Intent intent)
                {
                    Log.d("WLAN", "Receiving WLAN Scan results");

                    mScanResults = (ArrayList<ScanResult>) mWifiManager
                            .getScanResults();
                    JSONObject allFingerprints = new JSONObject();

                    //JSONObject collectedResults[] = new JSONObject[mScanResults.size()];
                    int res_ind = 0;
                    for (ScanResult result : mScanResults)
                    {
                        //Adding stuff to the JSON object wifiFingerprints at first
                        try
                        {
                            JSONObject singleResult = new JSONObject();
                            //wifiFingerprints.put("place_id", place_id);
                            singleResult.put("BSSID", result.BSSID);
                            singleResult.put("SSID", result.SSID);
                            singleResult.put("RSSI", result.level);

                            allFingerprints.put(Integer.toString(res_ind++), singleResult);
                        } catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                        Log.i("RESULT", result.BSSID + " " + result.level + " (" + startX + "," + startY + ")");
                        //osw.write("{" + startX + "," + startY + ": " + result.toString());
                        // TODO: Accumulate all results into one string before sending to server.
                    }
                    new SendToServer().execute(allFingerprints);
                    //mProgressDialog.dismiss();
                    unregisterReceiver(mBroadcastReceiver);
                    //saveResults.run();
                }
            };

            registerReceiver(mBroadcastReceiver, mIntentFilter);
            scanWifi.run();

        }
    }

    class SendToServer extends AsyncTask<JSONObject, Void, String>
    {
        @Override
        protected String doInBackground(JSONObject... params)
        {
            try
            {
                return makePostRequest(params[0]);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String coord)
        {
            // This is how to add a marker to the map.
            mapWebView.loadUrl("javascript:currentLocation.setLatLng(" + coord + "); currentLocation.update(); " +
                    "map.setView(" + coord + ")");
            Toast.makeText(MapWebView.this, "Curently at: " + coord, Toast.LENGTH_SHORT).show();
            //mProgressDialog.dismiss();
        }
    }
}
