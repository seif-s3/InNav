package com.innav.innav;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
public class MapWebView extends AppCompatActivity implements SensorEventListener
{
    private static final String LOG_TAG = "MAP_WEBVIEW";
    private static final String SERVER_BASE_URL = "http://mazein.herokuapp.com/";

    private static final String WIFI_SERVER_CONTROLLER = "finger_prints";
    private static final String MAGNETIC_SERVER_CONTROLLER = "magnetics";

    private static final String WIFI_SERVER_ACTION = "localization";
    private static final String MAGNETIC_SERVER_ACTION = "localization";

    private static final String WIFI_SERVER_FP_KEY = "finger_print";
    private static final String MAGNETIC_SERVER_FP_KEY = "magnetic";

    private static final String WIFI_SERVER_ROUTE = "localization.json";
    private static final String MAGNETIC_SERVER_ROUTE = "magnetic/localization.json";

    private WebView mapWebView;
    private ProgressDialog mProgressDialog;
    private ProgressDialog magneticProgressDialog;

    private WifiManager mWifiManager;
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mBroadcastReceiver;
    private ArrayList<ScanResult> mScanResults;
    private ArrayList<JSONObject> wifiFingerprints = new ArrayList<JSONObject>();

    private double[] magneticReadings = new double[3];
    private SensorManager mSensorManager;
    private Sensor mMagneticSensor;

    private static boolean RUN_LOCALIZATION = false;

    @Override
    public void onPause()
    {
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onResume()
    {
        mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == R.id.localize_option)
        {
            // Toggle Checked
            item.setChecked(!item.isChecked());
            RUN_LOCALIZATION = item.isChecked();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String place_id = getIntent().getStringExtra("place_name");
        Log.i("MAPVIEW_INTENT", place_id);
        setContentView(R.layout.map_webview);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).size() != 0)
        {
            mMagneticSensor = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0);
            mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        mapWebView = (WebView) findViewById(R.id.webView);
        WebSettings mapWebSettings = mapWebView.getSettings();
        mapWebSettings.setJavaScriptEnabled(true);
        mapWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        mapWebView.loadUrl("file:///android_asset/floor_plans/" + place_id + ".html");
    }

    private String makePostRequest(JSONObject finger_print,String pPath,
                                   String actionP, String controllerP, String fpKeyP) throws IOException
    {
        try
        {
            //final JSONobject which would be serialized to be sent to the server including wifiFingerprints JSONObject
            JSONObject requestJson = new JSONObject();
            requestJson.put("action", actionP);
            requestJson.put("controller", controllerP);
            requestJson.put(fpKeyP, finger_print);

            HttpURLConnection con = (HttpURLConnection) (new URL(SERVER_BASE_URL + pPath).openConnection());
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

    @Override
    public void onSensorChanged(SensorEvent e)
    {
        if (e.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
        {
            magneticReadings[0] = (double) e.values[0];
            magneticReadings[1] = (double) e.values[1];
            magneticReadings[2] = (double) e.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }


    // Class responsible for interfacing with the HTML files
    // Includes methods:
    //      - initializeWifiScan
    //      - initializeMagneticScan
    //      - intializeFingerprinting
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


        // TODO: Remove startX and startY
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
                            singleResult.put("place_id", place_id);
                            singleResult.put("BSSID", result.BSSID);
                            singleResult.put("SSID", result.SSID);
                            singleResult.put("RSSI", result.level);

                            allFingerprints.put(Integer.toString(res_ind++), singleResult);
                        } catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                        //osw.write("{" + startX + "," + startY + ": " + result.toString());
                        // TODO: Accumulate all results into one string before sending to server.
                    }
                    new SendToServer(WIFI_SERVER_ROUTE,
                            WIFI_SERVER_CONTROLLER,
                            WIFI_SERVER_ACTION,
                            WIFI_SERVER_FP_KEY).execute(allFingerprints);
                    //mProgressDialog.dismiss();
                    unregisterReceiver(mBroadcastReceiver);
                    //saveResults.run();
                }
            };

            registerReceiver(mBroadcastReceiver, mIntentFilter);
            scanWifi.run();

        }

        @JavascriptInterface
        public void initializeMagneticScan(final String place_id, final float startX, final float startY)
        {
            //magneticProgressDialog= ProgressDialog.show(MapWebView.this, "Magnetic Scan",
            //        "Scan at: \n" + String.valueOf(startX) + ", " + String.valueOf(startY), true);

            JSONObject magneticFingerprint = new JSONObject();
            JSONObject allMagneticFingerprints = new JSONObject();

            for(int scanNumber = 0; scanNumber < 10; scanNumber++)
            {
                // TODO: Add averaging and delay beteen readings
                double magnitude = Math.sqrt(
                        (magneticReadings[0] * magneticReadings[0])
                                + (magneticReadings[1] * magneticReadings[1])
                                + (magneticReadings[2] * magneticReadings[2])
                );
                try
                {
                    magneticFingerprint.put("place_id", place_id);
                    magneticFingerprint.put("x", magneticReadings[0]);
                    magneticFingerprint.put("y", magneticReadings[1]);
                    magneticFingerprint.put("z", magneticReadings[2]);
                    magneticFingerprint.put("magnitude", magnitude);
                    magneticFingerprint.put("angle", 90.0);
                    // TODO: Change inclination angle
                    allMagneticFingerprints.put(Integer.toString(scanNumber), magneticFingerprint);
                } catch (JSONException e)
                {
                    e.printStackTrace();
                    Log.e("MAGNETIC", "Json Error");
                }
            }

            new SendToServer(MAGNETIC_SERVER_ROUTE,
                    MAGNETIC_SERVER_CONTROLLER,
                    MAGNETIC_SERVER_ACTION,
                    MAGNETIC_SERVER_FP_KEY).execute(allMagneticFingerprints);

            //Toast.makeText(MapWebView.this, magneticFingerprint.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    class SendToServer extends AsyncTask<JSONObject, Void, String>
    {
        private ProgressDialog serverDialog;
        private String requestPath;
        private String controllerPath;
        private String action;
        private String fpKey;

        public SendToServer(String reqP, String controllerP, String actionP, String fpKeyP)
        {
            this.requestPath = reqP;
            this.controllerPath = controllerP;
            this.fpKey = fpKeyP;
            this.action = actionP;
        }
        @Override
        protected String doInBackground(JSONObject... params)
        {
            try
            {
                return makePostRequest(params[0],requestPath,action,controllerPath,fpKey);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String coord)
        {

            String calculations = "var wifi_lat = wiFiLocation.getLatLng().lat;\n" +
                    "           var wifi_lng = wiFiLocation.getLatLng().lng;\n" +
                    "           var mag_lat = magLocation.getLatLng().lat;\n" +
                    "           var mag_lng = magLocation.getLatLng().lng;\n" +
                    "\n" +
                    "           var wiFi_dist = Math.sqrt((wifi_lat-14)*(wifi_lat-14) + (wifi_lng-42)*(wifi_lng-42));\n" +
                    "           var mag_dist = Math.sqrt((mag_lat-14)*(mag_lat-14) + (mag_lng-42)*(mag_lng-42));\n" +
                    "\n" +
                    "           $(\"#info\").text(\"WiFi dist: \" + wiFi_dist + '\\n' + \"Mag dist: \" + mag_dist);\n";

            // This is how to add a marker to the map.
            if(requestPath == WIFI_SERVER_ROUTE)
            {
                mapWebView.loadUrl("javascript:wiFiLocation.setLatLng(" + coord + "); wiFiLocation.update(); " +
                        "map.setView(" + coord + ")");
            }
            else if(requestPath == MAGNETIC_SERVER_ROUTE)
            {
                mapWebView.loadUrl("javascript:magLocation.setLatLng(" + coord + "); magLocation.update(); " +
                        "map.setView(" + coord + ")");
            }
            mapWebView.loadUrl("javascript:"+calculations);
            Toast.makeText(MapWebView.this, "Curently at: " + coord, Toast.LENGTH_SHORT).show();
            //mProgressDialog.dismiss();
        }
    }
}
