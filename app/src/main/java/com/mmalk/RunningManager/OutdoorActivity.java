package com.mmalk.RunningManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class that handles GPS data
 */

public class OutdoorActivity extends NavigationDrawer implements ConnectionCallbacks, OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final String TAG = "OutdoorActivity";
    private static final int SECOND = 1000;
    private static final int FASTINTERVAL = 3 * SECOND;
    private static final int THEFASTESTINTERVAL = 1 * SECOND;
    private static final int MEDIUMINTERVAL = 7 * SECOND;
    private static final int SLOWINTERVAL = 10 * SECOND;
    private static final float SMALLDISPLACEMENT = 15f;
    private TextView distanceTextView;
    private TextView timeTextView;
    private ImageView imageViewAccuracy;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private int clean;
    private double distance;
    private double distanceDelta;
    private double latitude;
    private double longitude;
    Location locationOld;
    Location locationNew;

    private Button btnStartPause;
    private String comment;
    private int second;
    private DBHandler dbHandler;

    private Calendar calendar;
    private Timer timer;
    private boolean isRunning;
    private boolean isGpsFixed;
    private int gpsFixHelper;

    private NumberProgressBar numberProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        currentMenuItemPosition = 1;
        getLayoutInflater().inflate(R.layout.outdoor_activity, frameLayout);
        super.setTitle("Training");

        final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //notify user if GPS is turned off
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        calendar = Calendar.getInstance();

        //initialize variables
        second = 0;
        dbHandler = new DBHandler(this, null, null, 1);
        isRunning = false;
        isGpsFixed = false;
        gpsFixHelper = 0;

        //obtain references to UI elements
        distanceTextView = (TextView) findViewById(R.id.distance_textview);
        timeTextView = (TextView) findViewById(R.id.text_view_time_value);
        imageViewAccuracy = (ImageView) findViewById(R.id.image_view_accuracy);

        imageViewAccuracy.setVisibility(View.INVISIBLE);

        btnStartPause = (Button) findViewById(R.id.button_start_pause);
        btnStartPause.setText(getString(R.string.gps_not_fixed));
        btnStartPause.setBackgroundColor(getResources().getColor(R.color.gray_light));

        numberProgressBar = (NumberProgressBar) findViewById(R.id.progress_bar);

        distance = 0;
        distanceDelta = 0;
        clean = 0;
        locationOld = new Location("");
        locationNew = new Location("");

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    /**
     * do clean up when destroying the activity, that is
     * stop the timer
     * remove location updates
     */
    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (second > 0) {
            timer.cancel();
            timer.purge();
        }

        if (googleApiClient.isConnected()) {
            removeLocationUpdates();
            googleApiClient.disconnect();
        }
    }

    /**
     * method called when googleApiClient is being connected
     */
    @Override
    public void onConnected(Bundle bundle) {

        //request location updates
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(FASTINTERVAL);
        locationRequest.setFastestInterval(THEFASTESTINTERVAL);
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    /**
     * method called whenever a new location is received
     * @param location stands for the new location
     */
    @Override
    public void onLocationChanged(Location location) {

        //if GPS isn't working properly, wait until it works
        //first GPS locations are always inaccurate, so a good idea is to cancel them
        if(!isGpsFixed){
            fixGps(location);
            return;
        }

        /*if (clean < 11) {
            if (clean < 10) {
                if (location.getAccuracy() < 30 && clean > 2) {
                    clean = 10;
                    numberProgressBar.incrementProgressBy(100);
                    return;
                }
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                locationOld.setLatitude(latitude);
                locationOld.setLongitude(longitude);
                clean++;
                numberProgressBar.incrementProgressBy(10);
                return;
            } else {
                btnStartPause.setText("");
                btnStartPause.setBackgroundResource(R.drawable.ic_play_arrow_black_48dp);
                locationRequest.setInterval(10 * SECOND);
                locationRequest.setFastestInterval(8 * SECOND);
                clean = 12;

                numberProgressBar.setVisibility(View.INVISIBLE);
                imageViewAccuracy.setVisibility(View.VISIBLE);

                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (isRunning) {

                                    second++;
                                    timeTextView.setText(printTime(second));
                                }
                            }
                        });
                    }
                }, 1000, 1000);
            }
        }*/

        setAccuracyImageView(location.getAccuracy());

        //if the accuracy is bad, just wait for better one
        if (location.getAccuracy() > 30) {
            return;
        }

        //update the distance traveled between old and new locations
        //update UI elements
        if (isRunning) {

            locationOld.setLatitude(latitude);
            locationOld.setLongitude(longitude);

            latitude = location.getLatitude();
            longitude = location.getLongitude();

            locationNew.setLatitude(latitude);
            locationNew.setLongitude(longitude);

            distanceDelta = locationOld.distanceTo(locationNew);
            //the formatting below makes sure that distance is always in the format: 0.000km, so
            //if we multiply it by 1000 we will get correct distance in meters
            distanceDelta = ((int) distanceDelta * 1000) / 1000;
            distance += distanceDelta / 1000;

            distanceTextView.setText(String.format("%.3f", distance) + " km");
        }
    }

    /**
     * function used to start the measurements, when GPS is ready
     */
    private void fixGps(Location location){

        float accuracy = location.getAccuracy();

        if(accuracy < 30 && gpsFixHelper >= 3) {

            //if GPS is ready to use
            isGpsFixed = true;
            numberProgressBar.incrementProgressBy(100);

            btnStartPause.setText("");
            btnStartPause.setBackgroundResource(R.drawable.ic_play_arrow_black_48dp);

            //make the location update less frequent
            locationRequest.setInterval(SLOWINTERVAL);
            locationRequest.setFastestInterval(MEDIUMINTERVAL);
            locationRequest.setSmallestDisplacement(SMALLDISPLACEMENT);

            numberProgressBar.setVisibility(View.INVISIBLE);
            imageViewAccuracy.setVisibility(View.VISIBLE);

            //start timer
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (isRunning) {

                                second++;
                                timeTextView.setText(printTime(second));
                            }
                        }
                    });
                }
            }, 1000, 1000);
        }
        else{
            //wait until GPS will be ready
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            locationOld.setLatitude(latitude);
            locationOld.setLongitude(longitude);
            gpsFixHelper++;
            if(gpsFixHelper < 10)
                numberProgressBar.incrementProgressBy(10);
        }

    }

    /**
     * Ask user about exiting when he clicks the back button
     */
    @Override
    public void onBackPressed() {

        AlertDialog alertDialogResult = new AlertDialog.Builder(OutdoorActivity.this)
                .setMessage("Do you want to exit without saving?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        //cancel timer if is running
                        if(isRunning) {
                            timer.cancel();
                            timer.purge();
                        }

                        Intent intent = new Intent(OutdoorActivity.this, ResultView.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).create();
        alertDialogResult.show();
    }

    private void requestLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void removeLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    /**
     * function used to start or stop the timer, by manipulating isRunning variable
     */
    public void btnStartPauseClicked(View view) {

        if (isGpsFixed) {
            btnStartPause.setText("");
            if (isRunning) {
                isRunning = false;
                btnStartPause.setBackgroundResource(R.drawable.ic_play_arrow_black_48dp);
            } else {
                isRunning = true;
                btnStartPause.setBackgroundResource(R.drawable.ic_pause_black_48dp);
            }
        }
    }

    /**
     * Called when the user clicks the Save button
     */
    public void btnFinishClicked(View view) {

        isRunning = false;

        final long dateInMillis = calendar.getTimeInMillis();

        //create a layout for dialog box
        final EditText editText = new EditText(this);
        editText.setHint("Comment");

        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setPadding(
                getResources().getDimensionPixelSize(R.dimen.edit_text_padding),
                getResources().getDimensionPixelSize(R.dimen.edit_text_padding),
                getResources().getDimensionPixelSize(R.dimen.edit_text_padding),
                getResources().getDimensionPixelSize(R.dimen.edit_text_padding)
        );

        linearLayout.addView(editText);

        AlertDialog alertDialogComment = new AlertDialog.Builder(OutdoorActivity.this)
                .setTitle("Any comment?")
                .setView(linearLayout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        comment = editText.getText().toString();

                        AlertDialog alertDialogResult = new AlertDialog.Builder(OutdoorActivity.this)
                                .setTitle("Finish and save?")
                                .setMessage(getCurrentResult())
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                        if (second > 0) {
                                            timer.cancel();
                                            timer.purge();
                                        }

                                        Result result = new Result(
                                                (int) (distance * 1000),
                                                second,
                                                dateInMillis,
                                                comment
                                        );

                                        //save result in a database
                                        dbHandler.addResult(result);
                                        Toast.makeText(getApplicationContext(), "Save successful!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(OutdoorActivity.this, ResultView.class);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        Toast.makeText(getApplicationContext(), "Save cancelled!", Toast.LENGTH_SHORT).show();
                                        isRunning = true;
                                    }
                                }).create();
                        alertDialogResult.show();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isRunning = true;
                    }
                })
                .create();
        alertDialogComment.show();
    }

    /**
     * updates accuracy in UI accordingly to the given accuracy
     */
    private void setAccuracyImageView(float accuracy) {

        if (accuracy < 15) {
            imageViewAccuracy.setBackgroundColor(getResources().getColor(R.color.green));
        } else if (accuracy < 30) {
            imageViewAccuracy.setBackgroundColor(getResources().getColor(R.color.gray_light));
        } else if (accuracy < 60) {
            imageViewAccuracy.setBackgroundColor(getResources().getColor(R.color.red));
        } else {
            imageViewAccuracy.setBackgroundColor(getResources().getColor(R.color.black));
        }
    }

    /**
     * returns result in a nice printout
     */
    private String getCurrentResult() {

        String distanceS = "Distance:    " + String.format("%.03f", distance) + "km";
        String timeS = "\nTime:   " + printTime(second);
        String dateS = "\nDate:   " + getDate(calendar.getTimeInMillis());
        String commentS = "\nComment:   " + comment;

        if (comment.equals("")) {
            return distanceS + timeS + dateS;
        } else {
            return distanceS + timeS + dateS + commentS;
        }
    }

    /**
     *
     * @param s time counted by timer in seconds
     * @return time in format 00:00:00
     */
    private String printTime(int s) {

        int hour = s / 3600;
        int minute = (s - hour * 3600) / 60;
        int seconds = s - hour * 3600 - minute * 60;

        return String.format("%02d", hour)
                + ":" + String.format("%02d", minute)
                + ":" + String.format("%02d", seconds);
    }

    /**
     * function used to read date from milliseconds
     * it returns the date in format: "dd/MM/yyyy HH:mm"
     * for better understanding of format refer to SimpleDateFormat in docs.oracle.com
     */
    private String getDate(long millis) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return formatter.format(calendar.getTimeInMillis());
    }

    /**
     * shows dialog box about GPS status, and launches settings screen in mobile device if necessary
     */
    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        Intent intent = new Intent(OutdoorActivity.this, ResultView.class);
                        startActivity(intent);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
