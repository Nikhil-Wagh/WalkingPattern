package com.example.nikhil.walkingpattern;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
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
	private static final int PRIORITY_MAX = 2;
	private static final int ID_SERVICE = 1337;
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
        
        showNotification();

        /*Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentText("We are now collecting your walking pattern data. Please start walking")
                .setContentTitle("Walking Pattern")
                .setContentIntent(pendingIntent)
                .build();
	
		startForeground(1337, notification);*/
        return super.onStartCommand(intent, flags, startId);
    }
	
	private void showNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
		Notification notification = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setPriority(PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.setContentText("We are now collecting your walking pattern data. Please keep walking")
				.setContentTitle("Setting up your profile")
				.build();
		startForeground(ID_SERVICE, notification);
	}
	
	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(NotificationManager notificationManager) {
    	String channelId = "collect_data_service_channelId";
    	String channelName = "Collect Data Service";
		NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
		channel.setImportance(NotificationManager.IMPORTANCE_NONE);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		notificationManager.createNotificationChannel(channel);
 		return channelId;
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
        stopForeground(true);
        Log.d(TAG, "Service stopped");
    }
    
    private String getUserId() {
        return FirebaseAuth.getInstance().getUid();
    }
    
}
