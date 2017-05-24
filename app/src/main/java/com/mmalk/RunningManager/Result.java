package com.mmalk.RunningManager;

/**
 * Class representing single result
 */
public class Result {

    private int distance;
    private int time;
    private long date;
    private String comment;

    public Result(int distance, int time, long date, String comment) {
        this.distance = distance;
        this.time = time;
        this.date = date;
        this.comment = comment;
    }

    public Result() {

        this.distance = 0;
        this.time = 0;
        this.date = 0;
        this.comment = "";
    }

    public int getDistance() {
        return distance;
    }

    public int getTime() {
        return time;
    }

    public long getDate() {
        return date;
    }

    public String getComment() {
        return comment;
    }

    public String toString() {
        return distance + " " + time + " " + date +
                "\n" + comment + "\n";
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
