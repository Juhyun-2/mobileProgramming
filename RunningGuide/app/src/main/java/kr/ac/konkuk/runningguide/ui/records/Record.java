package kr.ac.konkuk.runningguide.ui.records;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;


//listView에 표시될 Record 객체를 정의
public class Record {
    private Drawable Image;
    private String Date;
    private String StartTime;
    private String FinishTime;
    private double TargetDistance;
    private double Distance;
    private int TargetTime;
    private int Time;
    private int Pace;


    public Record(@Nullable Drawable Image, String date, String startTime, String finishTime, double targetDistance, int targetTime, double distance,
                  int time, int pace){
        this.Image = Image;
        this.Date = date;
        this.StartTime = startTime;
        this.FinishTime = finishTime;
        this.TargetDistance = targetDistance;
        this.Distance = distance;
        this.TargetTime = targetTime;
        this.Time = time;
        this.Pace = pace;
    }

    public void setImage(Drawable image){
        this.Image = image;
    }

    public Drawable getImage() { return Image; }

    public String getDate() {
        return Date;
    }

    public String getStartTime() { return StartTime; }

    public String getFinishTime() { return FinishTime; }


    public double getTargetDistance() { return TargetDistance; }

    public int getTargetTime() { return TargetTime; }

    public double getDistance() {
        return Distance;
    }

    public int getTime() {
        return Time;
    }

    public int getPace() {
        return Pace;
    }
}
