package com.example.nikhil.walkingpattern;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import javax.annotation.Nullable;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Intent collectDataIntent;
    private final String TAG = "NavigationDrawer";
    private LineGraphSeries<DataPoint> mSeries;

    private String currentAxis = "";
    private final int X_AXIS_INDEX = 0;
    private final int Y_AXIS_INDEX = 1;
    private final int Z_AXIS_INDEX = 2;

    private Query getAccInOrder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
                Snackbar.make(view, "Data send toggled", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initNavHeader();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        collectDataIntent = new Intent(this, CollectDataService.class);
        GraphView graphView = findViewById(R.id.dynamicGraph);
        mSeries = new LineGraphSeries<>();
        graphView.addSeries(mSeries);


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        getAccInOrder = db.collection("AccelerometerReadings").orderBy("createdAt");

        getAccInOrder.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen Failed", e);
                }
                assert queryDocumentSnapshots != null;
                long minX = -1, prevX = -1, currX;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    if (doc.get("userId") != null) {
                        if (minX == -1)
                            minX = (long) doc.get("createdAt");
                        currX = (long) doc.get("createdAt") - minX;
                        if (currX > prevX) {
                            mSeries.appendData(new DataPoint(currX, (double) doc.get(getCurrentAxis())), false, 100);
                            prevX = currX;
                        } else {
                            Log.e(TAG, "next x-axis value is not greater n\n" + currX + " : " + prevX);
                        }
                        Log.i(TAG, "Loaded from server: " + doc.getData().toString());
                    }
                }
            }
        });
    }

    private void toggle() {
        if(isMyServiceRunning(CollectDataService.class)) {
            Log.d(TAG, "Service Stopped, user interrupt");
            stopService(collectDataIntent);
        }
        else {
            startService(collectDataIntent);
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initNavHeader() {
        FirebaseUser currentUser = null;
        try {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_test);
        ImageView userProfileImage = headerView.findViewById(R.id.profileImageView);
        TextView userNameTV = headerView.findViewById(R.id.userNameTextView);
        TextView userEmailIDTV = headerView.findViewById(R.id.userEmailIdTextView);

        assert currentUser != null;
        Glide.with(this).load(currentUser.getPhotoUrl()).into(userProfileImage);
        userNameTV.setText(currentUser.getDisplayName());
        userEmailIDTV.setText(currentUser.getEmail());

        MenuItem menuItem = navigationView.getMenu().findItem(R.id.checkboxX_axis);
        CompoundButton compundButton = (CompoundButton) menuItem.getActionView();
        compundButton.setChecked(true);


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    public void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.coordinatorLayoutGraphs), message, Snackbar.LENGTH_LONG).show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        final CompoundButton compundButton = (CompoundButton) item.getActionView();

        showSnackBar(String.valueOf(item.getGroupId()));
        if (id == R.id.checkboxX_axis) {
            if (compundButton.isChecked()) {
                setCurrentAxis(X_AXIS_INDEX);
            }
        }
        else if (id == R.id.checkboxY_axis) {
            if (compundButton.isChecked()) {
                setCurrentAxis(Y_AXIS_INDEX);
            }
        }
        else if (id == R.id.checkboxZ_axis) {
            if (compundButton.isChecked()) {
                setCurrentAxis(Z_AXIS_INDEX);
            }
        }

        if (shouldResetGraph()) {
            resetGraph();
        }
        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void resetGraph() {
        final DataPoint[] points = new DataPoint[1000];
        getAccInOrder.limit(1000).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult() != null && task.isSuccessful()) {
                            int index = 0;
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                points[index] = new DataPoint((float) doc.get("createdAtMillis"), (float) doc.get(getCurrentAxis()));
                                index++;
                            }
                        }
                        else {
                            Log.e(TAG, "Complete database GET FAILED", task.getException());
                        }
                    }
                });
        mSeries = new LineGraphSeries<>(points);
    }

    private boolean shouldResetGraph() {
        //TODO: Replace with proper logic
        return true;
    }

    private void setCurrentAxis(int axis) {
        if (axis == X_AXIS_INDEX)
            currentAxis = "x_axis";
        else if (axis == Y_AXIS_INDEX)
            currentAxis = "y_axis";
        else if (axis == Z_AXIS_INDEX)
            currentAxis = "z_axis";
        else
            currentAxis = "x_axis";
    }

    private String getCurrentAxis() {
        return currentAxis;
    }
}
