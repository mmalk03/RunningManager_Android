package com.mmalk.RunningManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Fragment through which user can save result manually
 */
public class ResultAdder extends FragmentActivity
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private static final String TAG = "ResultAdder";

    private EditText etComment;
    private TextView tvDate;

    private int distance;
    private int kmDistance;
    private int mDistance;

    private int time;
    private int hourTime;
    private int minTime;
    private int secTime;

    private String comment;

    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_adder);

        getActionBar().setTitle("Add result");

        etComment = (EditText) findViewById(R.id.edit_text_add_comment);

        calendar = Calendar.getInstance();

        tvDate = (TextView) findViewById(R.id.text_view_date_pick_value);
        tvDate.setText(getDate(calendar.getTimeInMillis()));
    }


    /**
     * method called after clicking save button
     */
    public void btnSaveClicked(View view) {

        //hide the keyboard
        View tempView = this.getCurrentFocus();
        if (tempView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        distance = kmDistance * 1000 + mDistance;
        time = hourTime * 3600 + minTime * 60 + secTime;
        comment = etComment.getText().toString();

        //display alert dialog with given result
        new AlertDialog.Builder(this)
                .setTitle("Save result?")
                .setMessage(getCurrentResult(calendar.getTimeInMillis()))
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Save successful!", Toast.LENGTH_SHORT).show();

                        Intent intent = makeIntent();
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Save canceled.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();

        etComment.setText("");
    }

    /**
     * method used to generate intent to switch from fragment to the calling activity
     * it returns the intent created, with additional fields containing information about result, that was made
     */
    private Intent makeIntent() {
        Intent intent = new Intent();
        intent.putExtra("distance", distance);
        intent.putExtra("time", time);
        intent.putExtra("comment", comment);
        intent.putExtra("date", calendar.getTimeInMillis());
        return intent;
    }

    /**
     * method displaying dialog box, through which user can provide distance
     */
    public void pickDistance(View view) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = this.getLayoutInflater();
        View dialogView = layoutInflater.inflate(R.layout.distance_picker, null);

        final NumberPicker kmPicker = (NumberPicker) dialogView.findViewById(R.id.kmPicker);
        final NumberPicker mPicker = (NumberPicker) dialogView.findViewById(R.id.mPicker);

        alertDialogBuilder.setView(dialogView);
        alertDialogBuilder.setTitle("Pick distance");
        alertDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                kmPicker.clearFocus();
                mPicker.clearFocus();

                TextView textView = (TextView) findViewById(R.id.text_view_distance_pick_value);
                textView.setText(String.valueOf(kmDistance) + "." + String.format("%03d", mDistance) + "km");
            }
        });

        kmPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        kmPicker.setMaxValue(20);
        kmPicker.setMinValue(0);
        kmPicker.setWrapSelectorWheel(true);
        kmPicker.setOnValueChangedListener(new kmChangeListener());

        //String[] values allows to display every 20th meter in mPicker, instead of incrementing it by 1
        String[] values = new String[50];
        for (int i = 0; i < 50; i++) {
            values[i] = String.format("%03d", i * 20);
        }

        mPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        mPicker.setMaxValue(49);
        mPicker.setMinValue(0);
        mPicker.setDisplayedValues(values);
        mPicker.setWrapSelectorWheel(true);
        mPicker.setOnValueChangedListener(new mChangeListener());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * method displaying dialog box through which user can provide time of his activity
     */
    public void pickTime(View view) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = this.getLayoutInflater();
        View dialogView = layoutInflater.inflate(R.layout.time_picker, null);

        final NumberPicker hourPicker = (NumberPicker) dialogView.findViewById(R.id.hourPicker);
        final NumberPicker minPicker = (NumberPicker) dialogView.findViewById(R.id.minPicker);
        final NumberPicker secPicker = (NumberPicker) dialogView.findViewById(R.id.secPicker);

        alertDialogBuilder.setView(dialogView);
        alertDialogBuilder.setTitle("Pick time");
        alertDialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                hourPicker.clearFocus();
                minPicker.clearFocus();
                secPicker.clearFocus();

                TextView textView = (TextView) findViewById(R.id.text_view_time_pick_value);
                textView.setText(printTime(hourTime, minTime, secTime));
            }
        });

        //String arrays allow to give custom values for pickers
        String[] hourValues = new String[11];
        for (int i = 0; i < 11; i++) {
            hourValues[i] = String.format("%02d", i);
        }

        String[] minValues = new String[61];
        for (int i = 0; i < 61; i++) {
            minValues[i] = String.format("%02d", i);
        }

        String[] secValues = new String[12];
        for (int i = 0; i < 12; i++) {
            secValues[i] = String.format("%02d", i * 5);
        }

        hourPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        hourPicker.setMaxValue(10);
        hourPicker.setMinValue(0);
        hourPicker.setDisplayedValues(hourValues);
        hourPicker.setWrapSelectorWheel(true);
        hourPicker.setOnValueChangedListener(new hourChangeListener());

        minPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        minPicker.setMaxValue(59);
        minPicker.setMinValue(0);
        minPicker.setDisplayedValues(minValues);
        minPicker.setWrapSelectorWheel(true);
        minPicker.setOnValueChangedListener(new minChangeListener());

        secPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        secPicker.setMaxValue(11);
        secPicker.setMinValue(0);
        secPicker.setDisplayedValues(secValues);
        secPicker.setWrapSelectorWheel(true);
        secPicker.setOnValueChangedListener(new secChangeListener());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * method displaying dialog box through which user can provide date of his activity
     */
    public void pickDate(View view) {

        final DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                ResultAdder.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.setVersion(DatePickerDialog.Version.VERSION_2);
        datePickerDialog.setCancelText("Cancel");
        datePickerDialog.vibrate(false);
        datePickerDialog.setYearRange(1970, calendar.get(Calendar.YEAR));
        datePickerDialog.setAccentColor(getResources().getColor(R.color.breathtaking_orange));
        datePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {

                TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                        ResultAdder.this,
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                );

                //show time picker after specifying the date
                timePickerDialog.setVersion(TimePickerDialog.Version.VERSION_2);
                timePickerDialog.setCancelText("Cancel");
                timePickerDialog.vibrate(false);
                timePickerDialog.setAccentColor(getResources().getColor(R.color.breathtaking_orange));
                timePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                    }
                });
                timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        tvDate.setText(getDate(calendar.getTimeInMillis()));
                    }
                });
                timePickerDialog.show(getFragmentManager(), "Time picker dialog");
            }
            //tvDate.setText(getDate(calendar.getTimeInMillis()));
        });
        datePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {

            }
        });
        datePickerDialog.show(getFragmentManager(), "Date picker dialog");
    }

    /**
     * interfaces below are used to modify appropriate variable (for exampke kmDistance)
     * after the user changes it through NumberPicker
     * newVal is the new value set in NumberPicker
     */
    private class kmChangeListener implements NumberPicker.OnValueChangeListener {

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

            kmDistance = newVal;
        }
    }

    private class mChangeListener implements NumberPicker.OnValueChangeListener {

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

            //multiply by 20 since we have only every 20th meter
            mDistance = newVal * 20;
        }
    }

    private class hourChangeListener implements NumberPicker.OnValueChangeListener {

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

            hourTime = newVal;
        }
    }

    private class minChangeListener implements NumberPicker.OnValueChangeListener {

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

            minTime = newVal;
        }
    }

    private class secChangeListener implements NumberPicker.OnValueChangeListener {

        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

            secTime = newVal * 5;
        }
    }

    /**
     * function called when date was chosen via DatePickerDialog
     * it modifies the private variable calendar accordingly
     */
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    /**
     * function called when time was chosen via TimePickerDialog
     * it modifies the private variable calendar accordingly
     */
    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {

        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
    }

    /**
     * function used for a better printout of time
     * returns String in format: 00:00:00
     */
    private String printTime(int h, int m, int s) {

        return String.format("%02d", h) + ":"
                + String.format("%02d", m) + ":"
                + String.format("%02d", s);
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
        return formatter.format(calendar.getTime());
    }

    /**
     * function used to obtain a nice printout of current result
     * it takes dateInMillis as a parameter, since it calls getDate(long), that requires the number of milliseconds
     * returns properly formatted String with result
     */
    private String getCurrentResult(long dateInMillis) {

        if(comment.equals("")){
            return "Distance:   " + kmDistance + "."
                    + String.format("%03d", mDistance) + "km"
                    + "\nTime:  " + printTime(hourTime, minTime, secTime)
                    + "\nDate:  " + getDate(dateInMillis);
        }
        else{
            return "Distance:   " + kmDistance + "."
                    + String.format("%03d", mDistance) + "km"
                    + "\nTime:  " + printTime(hourTime, minTime, secTime)
                    + "\nComment:   " + comment
                    + "\nDate:  " + getDate(dateInMillis);
        }
    }
}
