package com.example.nikhil.walkingpattern;

import java.util.Date;

public class SensorReadings {
    public float x_axis, y_axis, z_axis;
    public long createdAt;
    public String userId;

    /*SensorReadings() {

    }
*/
    SensorReadings(float[] readings, String userId) {
        this.x_axis = readings[0];
        this.y_axis = readings[1];
        this.z_axis = readings[2];
        this.createdAt = new Date().getTime();
        this.userId = userId;
    }

    /*public float getX_axis() {
        return x_axis;
    }

    public float getY_axis() {
        return y_axis;
    }

    public float getZ_axis() {
        return z_axis;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setX_axis(float x_axis) {
        this.x_axis = x_axis;
    }

    public void setY_axis(float y_axis) {
        this.y_axis = y_axis;
    }

    public void setZ_axis(float z_axis) {
        this.z_axis = z_axis;
    }*/
}

