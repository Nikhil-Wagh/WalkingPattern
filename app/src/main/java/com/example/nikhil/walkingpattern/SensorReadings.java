package com.example.nikhil.walkingpattern;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SensorReadings {
    public float x_axis, y_axis, z_axis;
    public long createdAtMillis;
    public String userId, createdAtISO;

    /*SensorReadings() {

    }
*/
    @SuppressLint("SimpleDateFormat")
    SensorReadings(float[] readings, String userId) {
        x_axis = readings[0];
        y_axis = readings[1];
        z_axis = readings[2];
        createdAtMillis = new Date().getTime();
        createdAtISO = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS").format(new Date().getTime());
        this.userId = userId;
    }
}

