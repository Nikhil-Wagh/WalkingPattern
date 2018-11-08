package com.example.nikhil.walkingpattern;

import java.util.Date;

public class SensorReadings {
    private String id;
    private float x_axis, y_axis, z_axis;
    private Date createdAt;
    private String userId;

    SensorReadings(float[] readings, String userId) {
        this.x_axis = readings[0];
        this.y_axis = readings[1];
        this.z_axis = readings[2];
        this.createdAt = new Date();
        this.userId = userId;
    }
}

