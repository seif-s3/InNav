package com.innav.innav;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment
{
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // slide menu items
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;

    private ArrayList<MainMenuItem> navDrawerItems;
    private MainMenuListAdapter adapter;

    private ArrayAdapter<String> mainMenuAdapter;

    public MainActivityFragment()
    {
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
        // TODO: Get number of Nearby Venues from server and add them dynamically here..
        navDrawerItems.add(new MainMenuItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1) ,true, "3" ));
        navDrawerItems.add(new MainMenuItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1) ));
        navDrawerItems.add(new MainMenuItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1) ));
        navDrawerItems.add(new MainMenuItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1) ));

        navMenuIcons.recycle();
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
}
