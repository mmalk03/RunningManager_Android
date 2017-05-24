package com.mmalk.RunningManager;

import android.content.Intent;
import android.os.Bundle;

/**
 * Class providing basic settings screen
 * the look of this screen is mainly defined in preferences.xml, according to
 * instructions provided at Android Developers -> Settings
 */
public class Settings extends NavigationDrawer {

    private static final String TAG = "Settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //currentMenuItemPosition is a field in super class,
        //it specifies the number in the menu
        currentMenuItemPosition = 2;
        getLayoutInflater().inflate(R.layout.settings, frameLayout);
        super.setTitle("Settings");
    }

    /**
     * pressing back button makes the user switch to the view with his results
     */
    @Override
    public void onBackPressed() {

        Intent intent = new Intent(this, ResultView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
