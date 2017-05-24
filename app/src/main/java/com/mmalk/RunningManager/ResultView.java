package com.mmalk.RunningManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.dift.ui.SwipeToAction;
import dmax.dialog.SpotsDialog;

/**
 * class responsible for the screen containing results
 * from layout linked with this class, results can be deleted,
 * sent to the server and a fragment for adding new result can be launched
 * implements DialogManagerInterface, through which networking threads exchange data with UI thread
 */

public class ResultView extends NavigationDrawer implements DialogManagerInterface{

    private static final String TAG = "ResultView";
    //Replace with server's MAC address
    private static final String SERVERMACADDRESS = "a1:b2:c3:d4:e5:f6";
    private static final String NETWORKFILE = "/proc/net/arp";
    private static final int VERTICAL_RECYCLER_SPACE = 48;
    private static final int SIDE_RECYCLER_SPACE = 36;
    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");

    private RecyclerView recyclerView;
    private ResultsAdapter adapter;
    private SwipeToAction swipeToAction;
    private DBHandler dbHandler;

    private Timer timer;
    private boolean timerIsRunning;
    private AlertDialog spotsDialog;
    private AlertDialog serverResponseDialog;

    private ResponseServer responseServer;
    private Client client;

    private List<Result> results = new LinkedList<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        currentMenuItemPosition = 0;
        getLayoutInflater().inflate(R.layout.recycler_results, frameLayout);
        super.setTitle("Results");

        //dbHandler used to delete or add new database entry
        dbHandler = new DBHandler(this, null, null, 1);

        //results array contains all results saved in database
        results.addAll(Arrays.asList(dbHandler.databaseToResultArray()));

        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);

        //adding bottom and side margin to recycler view items
        recyclerView.addItemDecoration(new RecyclerDecorator(
                VERTICAL_RECYCLER_SPACE, SIDE_RECYCLER_SPACE));

        //adding custom recycler view adapter
        adapter = new ResultsAdapter(results);
        recyclerView.setAdapter(adapter);

        //SwipeToAction is used to allow user interact with displayed results, by
        //swiping them to the left, right or clicking them
        swipeToAction = new SwipeToAction(recyclerView, new SwipeToAction.SwipeListener<Result>() {
            @Override
            public boolean swipeLeft(final Result itemData) {

                //ask if user wants to delete given element
                confirmDeletion(itemData);
                return true;
            }

            @Override
            public boolean swipeRight(Result itemData) {

                //displayComment after swiping right
                displayComment(itemData);
                return true;
            }

            @Override
            public void onClick(Result itemData) {
                onLongClick(itemData);
            }

            @Override
            public void onLongClick(final Result itemData) {

                //send data to server after clicking on result
                sendToServer(itemData);
            }
        });

        //FloatingActionButton is used to launch a fragment through which
        //user can create a custom result
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_add);
        fab.setColorNormal(getResources().getColor(R.color.breathtaking_orange));
        fab.setColorPressed(getResources().getColor(R.color.breathtaking_orange));
        fab.setColorRipple(getResources().getColor(R.color.breathtaking_orange));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //after starting the intent, this class will wait for its result
                Intent intent = new Intent(getApplicationContext(), ResultAdder.class);
                startActivityForResult(intent, 1);
            }
        });
    }

    /**
     * function used to send data to the server, if a connection is established
     * @param itemData stands for the result that will be sent to the server
     */
    private void sendToServer(final Result itemData) {

        //get values of shared preferences, for field pref_send
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean sendToServer = sharedPreferences.getBoolean(getString(R.string.key_pref_send), true);

        if(!sendToServer){
            return;
        }

        //check if the device has enabled internet hotspot
        if(isApEnabled()){

            //obtain servers ip if available
            String desktopIp = getDesktopIP();
            if(desktopIp.equals("")){

                new AlertDialog.Builder(ResultView.this)
                        .setMessage("Hotspot enabled, but server isn't connected.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                return;
                            }
                        })
                        .show();
            }
            else{
                //if the server is connected to hotspot, send result to it
                connectToServer(itemData, desktopIp);
                return;
            }
        }
        //if hotspot disabled, check if the device is connected to wifi,
        //because if the server and device are in the same network, sockets still can be used
        else if (checkWifi()) {

            //if device is connected to wifi, try sending data to server
            connectToServer(itemData, "");
            return;
        }
        else{
            //if turnOnWifi option was enabled in the Settings, turn wifi on
            boolean turnOnWifi = sharedPreferences.getBoolean(getString(R.string.key_pref_wifi), true);
            if(turnOnWifi) {

                //if user wants to turn on wifi, build dialog
                final AlertDialog wifiSpotsDialog = new SpotsDialog(ResultView.this, "Turning on Wi-Fi", R.style.Custom);
                wifiSpotsDialog.show();

                //turn on wifi
                final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);

                //chech every second if the device connected to wifi network
                timerIsRunning = true;
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (checkWifi()) {
                                    //if device successfully connected, dismiss the dialog and call sendToServer once again
                                    wifiSpotsDialog.dismiss();
                                    sendToServer(itemData);
                                    timerIsRunning = false;
                                    timer.cancel();
                                    timer.purge();
                                }
                            }
                        });
                    }
                }, 1000, 1000);

                //if after 10 seconds, the device hasn't connected to wifi, cancel the timer and dismiss dialog
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(timerIsRunning){
                            timerIsRunning = false;
                            timer.cancel();
                            timer.purge();
                            wifiSpotsDialog.cancel();
                        }
                    }
                }, 10000);
            }
            else{
                //display dialog about turning on wifi hotspot
                new AlertDialog.Builder(this)
                        .setMessage("Enable hotspot?")
                        .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //try turning on wifi
                                if(configApState()){
                                    new AlertDialog.Builder(ResultView.this)
                                            .setMessage("Hotspot enabled. Try sending data again.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })
                                            .show();
                                }
                                else{
                                    new AlertDialog.Builder(ResultView.this)
                                            .setMessage("Couldn't turn on WiFi Hotspot.")
                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {

                                                }
                                            })
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();
            }
        }
    }

    /**
     * function used to actually send the data to the server, if devices are reachable
     * @param itemData result to be sent
     * @param hotspotIP ip of server
     */
    private void connectToServer(final Result itemData, String hotspotIP){

        String defaultServerIP;
        String defaultServerPortS;
        int defaultServerPort;

        //get value of default port from shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultServerPortS = sharedPreferences.getString(getString(R.string.key_pref_port), "");
        defaultServerPort = Integer.valueOf(defaultServerPortS);

        if(hotspotIP.equals("")) {
            //get default server ip, if the one specified as parameter was empty
            defaultServerIP = sharedPreferences.getString(getString(R.string.key_pref_ip), "");
        }
        else{
            defaultServerIP = hotspotIP;
        }

        //build dialog layout for server ip and port
        final EditText editTextIp = new EditText(ResultView.this);
        final EditText editTextPort = new EditText(ResultView.this);

        editTextIp.setGravity(Gravity.CENTER_HORIZONTAL);
        editTextPort.setGravity(Gravity.CENTER_HORIZONTAL);

        editTextIp.setPadding(0, 50, 0, 50);
        editTextPort.setPadding(0, 50, 0, 50);

        Matcher matcher = IP_ADDRESS.matcher(defaultServerIP);
        if (matcher.matches()) {
            editTextIp.setText(defaultServerIP);
        } else {
            editTextIp.setHint("Server IP address");
        }

        if (defaultServerPort != 0) {
            editTextPort.setText(String.valueOf(defaultServerPort));
        } else {
            editTextPort.setHint("Server port");
        }

        LinearLayout linearLayout = new LinearLayout(ResultView.this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(layoutParams);

        linearLayout.addView(editTextIp);
        linearLayout.addView(editTextPort);

        new AlertDialog.Builder(ResultView.this)
                .setTitle("Server details")
                .setView(linearLayout)
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //after clicking send from the dialog:
                        //retrieve ip and port from editText
                        //create Client and ResponseServer objects to handle the connection
                        String serverIp = editTextIp.getText().toString();
                        String serverPort = editTextPort.getText().toString();

                        //show dialog that the data is being sent
                        spotsDialog = new SpotsDialog(ResultView.this, R.style.Custom);
                        spotsDialog.show();

                        client = new Client(serverIp, Integer.valueOf(serverPort), itemData, ResultView.this);
                        client.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);

                        responseServer = new ResponseServer(ResultView.this);
                        responseServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Toast.makeText(ResultView.this, "Sending to server cancelled!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     * function used to determine whether a comment should be displayed after swiping right
     * @param itemData result whose comment was to be displayed
     */
    private void displayComment(final Result itemData) {

        //display comment only if it isn't empty
        if (!itemData.getComment().equals("")) {
            new AlertDialog.Builder(ResultView.this)
                    .setTitle("Comment")
                    .setMessage(itemData.getComment())
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }

    /**
     * function used to remove result from the database using dbHandler
     * @param result stands for the result that has to be deleted
     */
    private void removeResult(Result result) {

        int pos = results.indexOf(result);
        //delete from database
        dbHandler.deleteResult(result.getDate());
        //delete from list with results
        results.remove(result);
        //remove from UI
        adapter.notifyItemRemoved(pos);
    }

    /**
     * ask user if given result has to be deleted
     * @param result used for displaying information about the result
     */
    private void confirmDeletion(final Result result) {

        new AlertDialog.Builder(this)
                .setTitle("Delete result?")
                .setMessage(getCurrentResult(result))
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Delete successful!", Toast.LENGTH_SHORT).show();
                        removeResult(result);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Deletion cancelled!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     *
     * @param result result that has to be properly formatted
     * @return properly formatted result as a String
     */
    private String getCurrentResult(final Result result) {

        int timeSeconds = result.getTime();
        int distanceMeters = result.getDistance();

        int hour = timeSeconds / 3600;
        int minute = (timeSeconds - hour * 3600) / 60;
        int seconds = timeSeconds - hour * 3600 - minute * 60;

        int kilometers = distanceMeters / 1000;
        int meters = distanceMeters - kilometers * 1000;

        String time = printTime(hour, minute, seconds);
        String distance = String.valueOf(kilometers) + "." + String.format("%03d", meters);
        String date = getDate(result.getDate());

        return "Distance:   " + distance + "km\n"
                + "Time:    " + time + "\n"
                + "Date:    " + date;
    }

    /**
     * formats time
     * @param h hours
     * @param m minutes
     * @param s seconds
     * @return properly formatted time, that is 00:00:00
     */
    private String printTime(int h, int m, int s) {

        return String.format("%02d", h) + ":"
                + String.format("%02d", m) + ":"
                + String.format("%02d", s);
    }

    /**
     * function used to retrieve custom result created with a fragment
     * @param requestCode identifier for given request
     * @param resultCode information whether the operation was successful
     * @param data intent containing fields of result, given by user
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {

                Bundle extras = data.getExtras();

                //create new result
                Result result = new Result(
                        extras.getInt("distance"),
                        extras.getInt("time"),
                        extras.getLong("date"),
                        extras.getString("comment")
                );

                //save result to the database
                dbHandler.addResult(result);
                //add result to the list with results
                results.add(result);
                //update UI
                adapter.notifyDataSetChanged();
                //recreate activity to properly display the new result list
                recreate();
            }
        }
    }

    /**
     * function used to format date
     * @param millis date in milliseconds
     * @return properly formatted date
     */
    private String getDate(long millis) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return formatter.format(calendar.getTime());
    }

    /**
     * checks if the device is connected to a wifi network
     * @return true if connected, false otherwise
     */
    private boolean checkWifi() {

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }

    /**
     * checks if the wifi hotspot is enabled on the device
     * @return true if it is, false otherwise
     */
    private boolean isApEnabled(){

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        try{
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        }
        catch (Throwable ignored){}
        return false;
    }

    /**
     * enable wifi hotspot
     * @return true if enabled, false otherwise
     */
    private boolean configApState(){

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        WifiConfiguration wifiConfiguration = null;
        try{
            if(isApEnabled()){
                wifiManager.setWifiEnabled(false);
            }
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, wifiConfiguration, !isApEnabled());
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * gets the ip address of server, if it's connected to the wifi hotspot
     * @return potential ip of a server according to /proc/net/arp
     * the return isn't always accurate, since the file mentioned above isn't updated when a device disconnects
     */
    private String getDesktopIP() {

        BufferedReader br;
        String desktopIp = "";
        try {
            //NETWORKFILE contains information about devices connected to the hotspot
            //it isn't updated frequently
            br = new BufferedReader(new FileReader(NETWORKFILE));
            String line;
            while ((line = br.readLine()) != null) {
                //split the entry from the NETWORKFILE
                //for each entry there
                String[] splitted = line.split(" +");
                if (splitted != null ) {

                    //obtain mac address and check if it matches the server mac
                    String mac = splitted[3];
                    Log.i(TAG, "Mac: " + mac);

                    if(mac.equals(SERVERMACADDRESS)) {
                        //obtain server ip
                        desktopIp = splitted[0];
                        Log.i(TAG, "IP: " + desktopIp);
                    }

                }
            }
        } catch(Exception e) {

        }
        return desktopIp;
    }

    /**
     * method through which networking threads can display a dialog about the connection
     * @param str message from the networking threads to be displayed
     */
    @Override
    public void updateDialog(String str) {

        //cancel dialog, which says "sending data to server"
        if(spotsDialog.isShowing()){
            spotsDialog.cancel();
        }

        if(serverResponseDialog != null) {
            if (serverResponseDialog.isShowing()) {
                serverResponseDialog.cancel();
            }
        }

        //build new dialog box
        serverResponseDialog = new AlertDialog.Builder(ResultView.this)
                .setMessage(str)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        serverResponseDialog.cancel();
                    }
                })
                .show();
    }
}
