package com.example.nikhil.walkingpattern;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SensorReadings {
    private String id;
    private double x_axis, y_axis, z_axis;
    private Date createdAt;
    private String userId;


    private FirebaseAuth mAuth;

    SensorReadings(double[] readings) {
        this.x_axis = readings[0];
        this.y_axis = readings[1];
        this.z_axis = readings[2];
        this.createdAt = new Date();

        try {
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            assert currentUser != null;
            this.userId = currentUser.getUid();
        } catch (Exception e) {
            Log.e("SensorReadings", e.getMessage());
        }
    }

    public Map<String, Object> getSensorReadings() {
        Map<String, Object> row = new HashMap<>();
        row.put("x_axis", x_axis);
        row.put("y_axis", y_axis);
        row.put("z_axis", z_axis);
        row.put("createdAt", createdAt);
        row.put("userId", userId);

        return row;
    }
}

