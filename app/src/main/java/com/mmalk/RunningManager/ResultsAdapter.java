package com.mmalk.RunningManager;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import co.dift.ui.SwipeToAction;

/**
 * custom recycler view adapter
 */
public class ResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Result> results;

    public class ResultViewHolder extends SwipeToAction.ViewHolder<Result> {

        public TextView timeView;
        public TextView distanceView;
        public TextView dateView;

        public ResultViewHolder(View v) {
            super(v);

            //obtain references to UI elements
            timeView = (TextView) v.findViewById(R.id.text_view_res_time);
            distanceView = (TextView) v.findViewById(R.id.text_view_res_distance);
            dateView = (TextView) v.findViewById(R.id.text_view_res_date);
        }
    }

    public ResultsAdapter(List<Result> results) {
        this.results = results;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //each row in this adapter is represented as result_view.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.result_view, parent, false);

        return new ResultViewHolder(view);
    }

    /**
     * function used to fill recylcer view items with data
     * @param holder
     * @param position position of element in the recycler view
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Result result = results.get(position);
        ResultViewHolder vh = (ResultViewHolder) holder;

        int timeSeconds = result.getTime();
        int hour = timeSeconds / 3600;
        int minute = (timeSeconds - hour * 3600) / 60;
        int seconds = timeSeconds - hour * 3600 - minute * 60;

        int distanceMeters = result.getDistance();
        int kilometers = distanceMeters / 1000;
        int meters = distanceMeters - kilometers * 1000;

        String time = String.format("%02d", hour)
                + ":" + String.format("%02d", minute)
                + ":" + String.format("%02d", seconds);

        String distance = String.valueOf(kilometers) + "." + String.format("%03d", meters);

        String date = getDate(result.getDate());

        vh.timeView.setText(time);
        vh.distanceView.setText(distance + "km");
        vh.dateView.setText(date);
        vh.data = result;
    }

    /**
     *
     * @param millis date in milliseconds
     * @return properly formatted date
     */
    private String getDate(long millis) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy" + "\n" + "HH:mm");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return formatter.format(calendar.getTime());
    }
}
