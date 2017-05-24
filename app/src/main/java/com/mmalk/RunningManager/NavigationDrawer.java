package com.mmalk.RunningManager;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

/**
 * abstract class representing menu appearing when swiping right, or clicking on appropriate button (top-left) corner
 * each class that contains the side menu, extends from this class
 */
public abstract class NavigationDrawer extends Activity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView drawerList;

    protected FrameLayout frameLayout;

    private CharSequence drawerTitle;
    private CharSequence title;
    private String[] activityTitles = {"Results", "Training", "Settings"};

    protected static int currentMenuItemPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer);

        //if no values for shared preferences were specified, here they are specified with the default ones
        //(default values are defined in R.xml.preferences for each entry
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //set default language
        Locale locale = new Locale("en_US");
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getApplicationContext().getResources().updateConfiguration(configuration, null);

        frameLayout = (FrameLayout) findViewById(R.id.content_frame);

        title = drawerTitle = getTitle();
        drawerLayout = (DrawerLayout) findViewById(R.id.navigation_drawer);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        drawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, activityTitles));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle navigation drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(title);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu();
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        menu.findItem(R.id.action_info).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //returns true, if it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //build text view for dialog box
        final TextView textView = new TextView(this);
        textView.setText(getString(R.string.about));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setTextSize(20);
        textView.setPadding(0, 50, 0, 50);

        switch (item.getItemId()) {
            case R.id.action_info:

                new AlertDialog.Builder(NavigationDrawer.this)
                        .setView(textView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * action to be taken after clicking one of navigation drawer items
     * in this case it closes the drawer and calls another method to start appropriate activity
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

            drawerLayout.closeDrawer(drawerList);
            //handler is used to reduce the lag when switching between activities, the UI is more fluent
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    selectItem(position);
                }
            }, 200);
        }
    }

    /**
     * method that starts appropriate activity via intent
     * position is the position in navigation drawer menu that was clicked
     */
    private void selectItem(int position) {

        //currentMenuItemPosition is the current opened menu item
        //the condition below doesn't let the user opened currently opened activity
        if(currentMenuItemPosition == position){
            drawerList.setItemChecked(currentMenuItemPosition, false);
            return;
        }

        //intent is added additional flag, so there isn't loop when pressing back button,
        // for example: settings->results->settings
        Intent intent = null;
        switch (position) {
            case 0:
                intent = new Intent(this, ResultView.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                drawerList.setItemChecked(position, true);
                break;
            case 1:
                intent = new Intent(this, OutdoorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                drawerList.setItemChecked(position, true);
                break;
            case 2:
                intent = new Intent(this, Settings.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                drawerList.setItemChecked(position, true);
                break;
            default:
                break;
        }
        if (intent != null) {
            startActivity(intent);
        }
        //setTitle(String.valueOf(position));
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getActionBar().setTitle(title);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
}
