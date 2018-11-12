package com.example.nikhil.walkingpattern;
/* TODO:
 * 1. Send Nav Drawer to back and toolbar to front
 * 2. Create it a pseudo tabbed activity
 * 3. Create a logo which shows the orientation of selected axis
 * 4. Create a Date Picker
 * 5. Eradicate all the static constant Strings
 * 6. Query for last hour only if not specified
 * */


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.icu.util.MeasureUnit;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
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
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import javax.annotation.Nullable;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Intent collectDataIntent;
    private final String TAG = "MainActivity";
    private LineGraphSeries<DataPoint> mSeries;

    private String currentAxis = "x_axis";
    private final int X_AXIS_INDEX = 0;
    private final int Y_AXIS_INDEX = 1;
    private final int Z_AXIS_INDEX = 2;

    private FirebaseFirestore db;
    private Query getAccInOrder;

    private CompoundButton[] radioButtons;
    private NavigationView navigationView;

    private long minX = 1000000000, prevX = -1, currX;
    private GraphView graphView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initUI(); // Initialize UI for this Acitivity

        initGraphView(); // Initialize Graph View and LineGraphSeries mSeries

        initFireBase(); // Get database instance and initialize database query

    }

    private void initFireBase() {
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        getAccInOrder = db.collection("AccelerometerReadings").orderBy("createdAtMillis");
        Log.i(TAG, "Firebase initialized");

        addSnapShotListener();
    }

    private void initGraphView() {
        collectDataIntent = new Intent(this, CollectDataService.class);
        graphView = findViewById(R.id.graph_view_MainActivity);
        mSeries = new LineGraphSeries<>();
        graphView.addSeries(mSeries);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(10000);
        graphView.getViewport().setScalable(true);

        graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    // TODO: show date format of events
                    return super.formatLabel(value, isValueX);
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        graphView.getGridLabelRenderer().setHorizontalLabelsAngle(30);
    }

    private void addSnapShotListener() {
        getAccInOrder.addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen Failed", e);
                }
                assert queryDocumentSnapshots != null;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    if (doc.get("userId") != null) {
                        minX = Math.min(minX, (long) doc.get("createdAtMillis"));
                        currX = (long) doc.get("createdAtMillis") - minX;
                        if (currX > prevX) {
                            mSeries.appendData(new DataPoint(currX, (double) doc.get(getCurrentAxis())), true, 100);
                            prevX = currX;
                        }
                    }
                }
            }
        });
    }

    private void initUI() {
        /* Initialize Floating Action Bar,
        Navigation Header (nav_header_test.xml)
        and final Navigation Drawer */
        initFAB();
        initNavView();
        initNavHeader();
    }


    private void initNavView() {
        Toolbar toolbar = findViewById(R.id.toolbar_MainActivity);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view_MainActivity);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void toggle() {
        if(isMyServiceRunning(CollectDataService.class)) {
            Log.d(TAG, "Service Stopped, user interrupt");
            stopService(collectDataIntent);
            showSnackBar("Service Stopped.");
        }
        else {
            startService(collectDataIntent);
        }
    }

    public void initFAB() {
        FloatingActionButton fab = findViewById(R.id.fab_MainActivity);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
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

        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_test);
        ImageView userProfileImage = headerView.findViewById(R.id.circular_image_view_profile_pic_NavigationHeader);
        TextView userNameTV = headerView.findViewById(R.id.text_view_username_NavigationHeader);
        TextView userEmailIDTV = headerView.findViewById(R.id.text_view_emailid_NavigationHeader);

        assert currentUser != null;
        Glide.with(this).load(currentUser.getPhotoUrl()).into(userProfileImage);
        userNameTV.setText(currentUser.getDisplayName());
        userEmailIDTV.setText(currentUser.getEmail());

        initRadioButtons(navigationView);
    }

    private void setRadioButtonDefaultChecked(NavigationView navigationView) {
        MenuItem menuItem = navigationView.getMenu().findItem(R.id.radio_buttonX_axis);
        RadioButton radioButton = (RadioButton) menuItem.getActionView();
        radioButton.setChecked(true);
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

    public void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.coordinatorLayout_MainActivity), message, Snackbar.LENGTH_LONG).show();
    }

    public void onPostRadioButtonChanged(int radioButtonIndex, boolean shouldResetGraph) {
        setCurrentAxis(radioButtonIndex);
        resetGraph(shouldResetGraph);
    }

//    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (item.getGroupId() == R.id.radio_buttons_accelerometer) {
            // Behaviour of Radio Buttons
            final RadioButton radioButton = (RadioButton) item.getActionView();
            if (id == R.id.radio_buttonX_axis) {
                onMenuItemSelectedBehaviour(radioButton, X_AXIS_INDEX);
            } else if (id == R.id.radio_buttonY_axis) {
                onMenuItemSelectedBehaviour(radioButton, Y_AXIS_INDEX);
            } else if (id == R.id.radio_buttonZ_axis) {
                onMenuItemSelectedBehaviour(radioButton, Z_AXIS_INDEX);
            }
        }

        if (id == R.id.button_download_data_ActivityDrawer) {
            showSnackBar("Downloading Data now");
        }
        else if (id == R.id.button_logout_ActivityDrawer) {
            logout();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        //TODO: Logout User here
    }

    private boolean onMenuItemSelectedBehaviour(CompoundButton compundButton, int index) {
        if (compundButton.isChecked()) {
            showSnackBar("Atleast one of the axis has to be set");
            return false;
        }
        for (CompoundButton compoundButton: radioButtons) {
            compoundButton.setChecked(false);
        }
        radioButtons[index].setChecked(true);
        return true;
    }

    private void resetGraph(boolean shouldResetGraph) {
        Log.i(TAG, "Reset graph: " + shouldResetGraph);
        if (shouldResetGraph) {
            graphView.setTitle("Accelerometer: " + getCurrentAxis());
            getAccInOrder.limit(1000).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "Size of data recieved:" + task.getResult().size());
                        if (task.getResult() != null && task.isSuccessful()) {
                            int index = 0;
                            DataPoint[] points = new DataPoint[1000];
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                DataPoint dataPoint = new DataPoint((long) doc.get("createdAtMillis"), (double) doc.get(getCurrentAxis()));
                                points[index] = dataPoint;
                                index++;
                            }
                            mSeries.resetData(points);
                        }
                        else {
                            Log.e(TAG, "Complete database GET FAILED", task.getException());
                        }
                    }
                });
        }
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
        Log.i(TAG, "Current Axis changed to: " + getCurrentAxis());
    }

    private String getCurrentAxis() {
        return currentAxis;
    }

    private void initRadioButtons(NavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        radioButtons = new CompoundButton[]{
                (CompoundButton) menu.findItem(R.id.radio_buttonX_axis).getActionView(),
                (CompoundButton) menu.findItem(R.id.radio_buttonY_axis).getActionView(),
                (CompoundButton) menu.findItem(R.id.radio_buttonZ_axis).getActionView()
        };
        final MenuItem[] menuItems = new MenuItem[]{
                menu.findItem(R.id.radio_buttonX_axis),
                menu.findItem(R.id.radio_buttonY_axis),
                menu.findItem(R.id.radio_buttonZ_axis)
        };
        setRadioButtonDefaultChecked(navigationView);
        for (final MenuItem item : menuItems) {
            CompoundButton compoundButton = (CompoundButton) item.getActionView();
            compoundButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        int index = 0;
                        for (MenuItem item : menuItems) {
                            CompoundButton radioButton = (CompoundButton) item.getActionView();
                            if (radioButton.getId() == buttonView.getId()) {
                                item.setChecked(true);
                                radioButton.setChecked(true);
                                onPostRadioButtonChanged(index, true);
                            } else {
                                item.setChecked(false);
                                radioButton.setChecked(false);
                            }
                            index++;
                            DrawerLayout drawer = findViewById(R.id.drawer_layout);
                            drawer.closeDrawer(GravityCompat.START);
                        }
                        Log.i(TAG, "Current Axis: " + getCurrentAxis());
                    }
                }
            });
        }
    }
}
