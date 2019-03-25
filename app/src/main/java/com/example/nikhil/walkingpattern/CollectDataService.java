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
import android.os.VibrationEffect;
import android.os.Vibrator;
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

public class CollectDataService extends Service implements SensorEventListener {
	private static final int PRIORITY_MAX = 2;
	private static final int ID_SERVICE = 1337;
	private SensorManager mSensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private FirebaseFirestore db;

    private FirebaseUser currentUser;

    private final String TAG = "CollectDataService";
	String ACC_COLLECTION = "AccelerometerReadings";
	String GYRO_COLLECTION = "GyroscopeReadings";
	

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
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        db = FirebaseFirestore.getInstance();
        try {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        if (registerSensors()) {
			showNotification();
			autoTerminate();
		}
		
        return super.onStartCommand(intent, flags, startId);
    }
	
	private void autoTerminate() {
    	final long runTime = 60 * 1000;//2 * 60 * 1000;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(runTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				stopSelf();
			}
		}).start();
	}
	
	private void unregisterSensors() {
    	mSensorManager.unregisterListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
    	mSensorManager.unregisterListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
    	Log.i(TAG, "Sensors unregistered.");
	}
	
	private void showNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
		Notification notification = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.drawable.ic_app_notification)
				.setPriority(PRIORITY_MAX)
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.setContentText("Please keep walking.")
				.setContentTitle("Setting up your profile")
				.build();
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class),
				PendingIntent.FLAG_UPDATE_CURRENT);
		notificationBuilder.setContentIntent(pendingIntent);
		startForeground(ID_SERVICE, notification);
	}
	
	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(NotificationManager notificationManager) {
    	String channelId = "collect_data_service_channelId";
    	String channelName = "Collect Data Service";
		NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
		channel.setImportance(NotificationManager.IMPORTANCE_NONE);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		channel.setShowBadge(true);
		notificationManager.createNotificationChannel(channel);
 		return channelId;
	}
	
	private boolean registerSensors() {
    	if (mSensorManager.registerListener(
    			this, accSensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH)){
    		if (mSensorManager.registerListener(
    				this, gyroSensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH)) {
    			Log.i(TAG, "Sensors registered successfully.");
    			return true;
			}
		}
		Log.e(TAG, "Sensors could not be registered, aborting.");
    	return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//            Log.i(TAG, "Sensor changed, values are: " + Arrays.toString(event.values));
            toFirebaseFireStore(event.values, ACC_COLLECTION);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//			Log.i(TAG, "Sensor changed, values are: " + Arrays.toString(event.values));
			toFirebaseFireStore(event.values, GYRO_COLLECTION);
		}
    }

    private void toFirebaseFireStore(float[] values, String path) {
        SensorReadings accelerometerReadings = new SensorReadings(values, currentUser.getUid());
        db.collection("AppData").document(getUserId())
                .collection(path)
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
        unregisterSensors();
        stopForeground(true);
        vibrate();
        Log.d(TAG, "Service stopped");
    }
	
	private void vibrate() {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
		} else {
			v.vibrate(500);
		}
	}
	
	private String getUserId() {
        return FirebaseAuth.getInstance().getUid();
    }
    
}
