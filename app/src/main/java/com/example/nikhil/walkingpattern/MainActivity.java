package com.example.nikhil.walkingpattern;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private Intent collectDataIntent;
    private Button sendDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        collectDataIntent = new Intent(this, CollectDataService.class);

        sendDataButton = (findViewById(R.id.send_data_button));
        sendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        final LinearLayout linearLayout = findViewById(R.id.cloudDataLayout);



        FirebaseFirestore db = FirebaseFirestore.getInstance();
        /*FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);*/
        CollectionReference accCollectionReference = db.collection("AccelerometerReadings");
        accCollectionReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen Failed", e);
                }

//                List<SensorReadings> sensorReadings = new ArrayList<>();
                assert queryDocumentSnapshots != null;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    if (doc.get("userId") != null) {
                        TextView temp = new TextView(MainActivity.this);
                        temp.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        temp.setText("Created at: " + doc.getData().toString());
                        linearLayout.addView(temp);
//                        Log.i(TAG, doc.getData().toString());
                    }
                }
            }
        });
    }

    private void toggle() {
        if(isMyServiceRunning(CollectDataService.class)) {
            Log.d(TAG, "Service Stopped, user interrupt");
            stopService(collectDataIntent);
            sendDataButton.setText("SEND ACCELEROMETER DATA");
        }
        else {
            startService(collectDataIntent);
            sendDataButton.setText("STOP IT!!");
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
