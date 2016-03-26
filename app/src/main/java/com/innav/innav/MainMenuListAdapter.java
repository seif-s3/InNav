package com.innav.innav;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainMenuListAdapter extends BaseAdapter
{
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;
    private Context context;
    private ArrayList<MainMenuItem> MainMenuItems = new ArrayList<>();

    public MainMenuListAdapter(Context context, int numberOfNearbyVenues)
    {
        this.context = context;
        navMenuIcons = this.context.getResources().obtainTypedArray(R.array.main_menu_icons);
        navMenuTitles = this.context.getResources().getStringArray(R.array.main_menu_items);

        MainMenuItems.add(new MainMenuItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1),
                true, Integer.toString(numberOfNearbyVenues)));
        MainMenuItems.add(new MainMenuItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
        MainMenuItems.add(new MainMenuItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
        MainMenuItems.add(new MainMenuItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));

    }

    public void updateNearbyVenuesNumber(Integer numNear)
    {
        MainMenuItems.clear();
        MainMenuItems.add(new MainMenuItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1),
                true, numNear.toString()));
        MainMenuItems.add(new MainMenuItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
        MainMenuItems.add(new MainMenuItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
        //MainMenuItems.add(new MainMenuItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));

    }

    @Override
    public int getCount() {
        return MainMenuItems.size();
    }

    @Override
    public Object getItem(int position) {
        return MainMenuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.main_menu_list_item, null);
        }

        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.main_menu_item_icon);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.main_menu_item_text);
        TextView txtCount = (TextView) convertView.findViewById(R.id.main_menu_item_counter);

        imgIcon.setImageResource(MainMenuItems.get(position).getIcon());
        txtTitle.setText(MainMenuItems.get(position).getTitle());

        // displaying count
        // check whether it set visible or not
        if(MainMenuItems.get(position).getCounterVisibility()){
            txtCount.setText(MainMenuItems.get(position).getCount());
        }else{
            // hide the counter view
            txtCount.setVisibility(View.GONE);
        }

        return convertView;
    }

}