package com.example.nikhil.walkingpattern;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class CollectDataService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private FirebaseFirestore db;

    private FirebaseUser currentUser;

    private final String TAG = "CollectDataService";

    public CollectDataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        db = FirebaseFirestore.getInstance();
        try {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        registerSensors();

//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//        Notification notification = new NotificationCompat.Builder(this)
//                .setContentText("We are now collecting your walking pattern data. Please start walking")
//                .setContentTitle("Walking Pattern")
//                .setContentIntent(pendingIntent)
//                .build();

        //startForeground(1337, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerSensors() {
        Log.i(TAG, "Sensors registered:" + mSensor.getName());
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            Log.i(TAG, "Sensor changed, values are: " + Arrays.toString(event.values));
            toFirebaseFireStore(event.values);
        }
    }

    private void toFirebaseFireStore(float[] values) {
        SensorReadings accelerometerReadings = new SensorReadings(values, currentUser.getUid());
        String ACCELEROMETERPATH = "AccelerometerReadings";
        db.collection("AppData").document(getUserId())
                .collection(ACCELEROMETERPATH)
                .add(accelerometerReadings)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Document snapshot added with ID:" + documentReference.getId());
                    }
                });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, sensor.getName() + " accuracy changed to " + accuracy);
        Toast.makeText(this, "Sensor accuracy changed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this, mSensor);
        Log.d(TAG, "Service stopped");
    }
    
    private String getUserId() {
        return FirebaseAuth.getInstance().getUid();
    }
    
}
