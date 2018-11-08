package com.example.nikhil.walkingpattern;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final Intent collectDataIntent = new Intent(this, CollectDataService.class);

        Button sendDataButton = (findViewById(R.id.send_data_button));
        sendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(collectDataIntent);
                Log.d(TAG, "Service stopped by User interupt");
            }
        });

        CollectionReference accCollectionReference = db.collection("AccelerometerReadings");
        accCollectionReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen Failed", e);
                }

//                List<SensorReadings> sensorReadings = new ArrayList<>();
                for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                    if (doc.get("userId") != null) {
                        Log.i(TAG, doc.getData().toString());
                    }
                }
            }
        });
        startService(collectDataIntent);
    }
}
