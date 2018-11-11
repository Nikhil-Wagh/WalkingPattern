package com.example.nikhil.walkingpattern;
/* TODO:
 * 1. Send Nav Drawer to back and toolbar to front
 * 2. Create it a pseudo tabbed activity
 * 3. Create a logo which shows the orientation of selected axis
 * */


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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import java.security.acl.Group;

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

    private RadioButton[] radioButtons;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initUI(); // Initialize UI for this Acitivity

        initGraphView(); // Initialize Graph View and LineGraphSeries mSeries

        initFireBase(); // Get database instance and initialize database query

        addSnapShotListener();
    }

    private void initFireBase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        getAccInOrder = db.collection("AccelerometerReadings").orderBy("createdAt");
    }

    private void initGraphView() {
        collectDataIntent = new Intent(this, CollectDataService.class);
        GraphView graphView = findViewById(R.id.graph_view_MainActivity);
        mSeries = new LineGraphSeries<>();
        graphView.addSeries(mSeries);
    }

    private void addSnapShotListener() {
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

    private void initUI() {
        /* Initialize Floating Action Bar,
        Navigation Header (nav_header_test.xml)
        and final Navigation Drawer */
        initFAB();
        initNavHeader();
        initNavView();
    }


    private void initNavView() {
        Toolbar toolbar = findViewById(R.id.toolbar_MainActivity);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view_MainActivity);
        navigationView.setNavigationItemSelectedListener(this);
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

    public void initFAB() {
        FloatingActionButton fab = findViewById(R.id.fab_MainActivity);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
                Snackbar.make(view, "Data send toggled", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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


        NavigationView navigationView = findViewById(R.id.nav_view_MainActivity);
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
        Snackbar.make(findViewById(R.id.coordinatorLayout_MainActivity), message, Snackbar.LENGTH_LONG).show();
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
                resetGraph(onRadioButtonCheckedBehaviour(radioButton, X_AXIS_INDEX));
            } else if (id == R.id.radio_buttonY_axis) {
                resetGraph(onRadioButtonCheckedBehaviour(radioButton, Y_AXIS_INDEX));
            } else if (id == R.id.radio_buttonZ_axis) {
                resetGraph(onRadioButtonCheckedBehaviour(radioButton, Z_AXIS_INDEX));
            }
        }

        else if (id == R.id.button_download_data_ActivityDrawer) {
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

    private boolean onRadioButtonCheckedBehaviour(CompoundButton compundButton, int index) {
        if (compundButton.isChecked()) {
            showSnackBar("Atleast one of the axis has to be set");
            return true;
        }
        setCurrentAxis(index);
        for (RadioButton radioButton: radioButtons) {
            radioButton.setChecked(false);
        }
        radioButtons[index].setChecked(true);
        return false;
    }

    private void resetGraph(boolean shouldResetGraph) {
        if (shouldResetGraph) {
//        TODO: Error in this part
//        mSeries.resetData(getPointsFromFireBase());
        }
    }

    private DataPoint[] getPointsFromFireBase() {
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
        return points;
    }

    private boolean shouldResetGraph() {
        //TODO: Replace with proper logic
        return false;
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

    private void initRadioButtons(NavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        radioButtons = new RadioButton[]{
                (RadioButton) menu.findItem(R.id.radio_buttonX_axis).getActionView(),
                (RadioButton) menu.findItem(R.id.radio_buttonY_axis).getActionView(),
                (RadioButton) menu.findItem(R.id.radio_buttonZ_axis).getActionView()
        };
        setRadioButtonDefaultChecked(navigationView);
/*
        Menu testMenu = navigationView.getMenu();
        testMenu.add(R.id.radio_buttons_accelerometer, testMenu.findItem(R.id.radio_buttonX_axis).getActionView().getId(), 1, "Test Title");
        testMenu.add(R.id.radio_buttons_accelerometer, testMenu.findItem(R.id.radio_buttonY_axis).getActionView().getId(), 1, "Test Title");
*/

       /* Menu testMenu = navigationView.getMenu();
        RadioGroup radioGroup = new RadioGroup(navigationView.getContext());
        for (int i = 0; i < 3; i++) {
            RadioButton radioButton = new RadioButton(navigationView.getContext());
            radioButton.setText("Show X - Axis");
            radioGroup.addView(radioButton);
//            testMenu.add(radioGroup.getId(), radioButton.getId(), RadioGroup.VERTICAL, "Show X Axis");
        }
*/

       /* navigationView.getMenu().removeGroup(R.id.radio_buttons_accelerometer);
        RadioGroup radioGroup = new RadioGroup(navigationView.getContext());
        for (int i = 0; i < 3; i++) {
            RadioButton radioButton = new RadioButton(navigationView.getContext());
            radioButton.setText("Show X - Axis");
            radioGroup.addView(radioButton);
        }
        navigationView.addView(radioGroup);*/

        /*int[] ids = {R.id.radio_buttonX_axis, R.id.radio_buttonY_axis, R.id.radio_buttonZ_axis};
        RadioGroup radioGroup = new RadioGroup(navigationView.getContext());
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        for (int id : ids) {
            MenuItem menuItem = navigationView.getMenu().findItem(id);
            RadioButton radioButton = (RadioButton) menuItem.getActionView();
            radioGroup.addView(radioButton);
        }*/
    }
}
