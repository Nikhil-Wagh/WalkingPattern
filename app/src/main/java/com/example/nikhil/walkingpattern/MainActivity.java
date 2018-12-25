package com.example.nikhil.walkingpattern;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Intent collectDataIntent;
    private final String TAG = "MainActivity";
    private LineGraphSeries<DataPoint> mSeries;
	private TextView graphTitle;

    private String currentAxis = "x_axis";
    private final int X_AXIS_INDEX = 0;
    private final int Y_AXIS_INDEX = 1;
    private final int Z_AXIS_INDEX = 2;

    private Query getAccInOrder;

    private CompoundButton[] radioButtons;
    private NavigationView navigationView;

    private long prevX = -1, currX;

    private ProgressBar progressBar;
    private FloatingActionButton fab;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initUI(); // Initialize UI for this Acitivity

        initGraphView(); // Initialize Graph View and LineGraphSeries mSeries

        initFireBase(); // Get database instance and initialize database query
		doForTargets();
    }
	
	private void doForTargets() {
    	if (firstRun()) {
    		showTargets();
		}
	}
	
	private boolean firstRun() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(!prefs.getBoolean("firstTime", false)) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("firstTime", true);
			editor.apply();
			return true;
		}
		return false;
	}
	
	private void showTargets() {
		final Drawable droid = ContextCompat.getDrawable(this, R.drawable.ic_chart_line);
    	new TapTargetSequence(this).
				targets(
						TapTarget.forToolbarNavigationIcon((Toolbar) findViewById(R.id.toolbar_MainActivity),
								getString(R.string.tap_target_navigation_icon_title),
								getString(R.string.tap_target_navigation_icon_description))
								.cancelable(false),
						TapTarget.forView(
								findViewById(R.id.fab_MainActivity),
								getString(R.string.tap_target_fab_title),
								getString(R.string.tap_target_fab_description))
								.transparentTarget(true)
								.cancelable(false),
						TapTarget.forView(
								findViewById(R.id.graph_view_MainActivity),
								getString(R.string.tap_target_graphs_title),
								getString(R.string.tap_target_graphs_description))
								.targetRadius(60)
								.icon(droid)
								.cancelable(false)
				).start();
	}
	
	private void initFireBase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
//        lowerLimitTime = new Date().getTime() - 600 * 1000;
        long lowerLimitTime = new Date().getTime() - 300 * 1000; // last 5 min
        Log.i(TAG, "Lower limit of X values: " + lowerLimitTime);
        
		String appCollectionPath = getString(R.string.app_collection_path);
		String accCollectionPath = getString(R.string.acc_collection_path);
		String orderByParameter = getString(R.string.order_by_parameter);
		String greaterThanParameter = getString(R.string.where_greater_than_parameter);
		
		getAccInOrder = db.collection(appCollectionPath).document(getUserId())
				.collection(accCollectionPath)
				.orderBy(orderByParameter)
				.whereGreaterThan(greaterThanParameter, lowerLimitTime);
        Log.i(TAG, "Firebase initialized");

        addSnapShotListener();
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		addSnapShotListener();
	}
	
	private void initGraphView() {
        collectDataIntent = new Intent(this, CollectDataService.class);
		GraphView graphView = findViewById(R.id.graph_view_MainActivity);
        mSeries = new LineGraphSeries<>();
        graphView.addSeries(mSeries);
        graphView.getViewport().setXAxisBoundsManual(true);
//        graphView.getViewport().setMinX(0);
//        graphView.getViewport().setMaxX(1000);
        graphView.getViewport().setScalable(true);

        String dateformat = getString(R.string.database_date_format);
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateformat);
		
		
/*
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMaximumIntegerDigits(2);
		graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(nf, nf)); // This overrides the previous LabelFormatter
*/
		graphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this, simpleDateFormat));
		
		// If I try to set them both together. I get this error.
		//graphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this, simpleDateFormat), nf);
		//setLabelFormatter (LabelFormatter) in GridLabelRenderer cannot be applied to (DateAsXAxisLabelFormatter, java.text.NumberFormat)
		
		graphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(MainActivity.this, simpleDateFormat));
        graphView.getGridLabelRenderer().setPadding(120);
        graphView.getGridLabelRenderer().setLabelVerticalWidth(50);
		
		graphView.setHorizontalScrollBarEnabled(true);
        graphView.getGridLabelRenderer().setHorizontalLabelsAngle(30);
        graphTitle = findViewById(R.id.text_view_graph_heading);
        graphTitle.setText(getGraphTitle());
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
        progressBar = findViewById(R.id.progressBar);
    }

    private void addSnapShotListener() {
        getAccInOrder.addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen Failed", e);
                }
                else {
					if (queryDocumentSnapshots != null) {
						for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
							if (doc.get("userId") != null) {
								currX = (long) doc.get(getString(R.string.order_by_parameter));
								if (currX > prevX) {
									mSeries.appendData(new DataPoint(currX, (double) doc.get(getCurrentAxis())), true, 100);
									prevX = currX;
								}
							}
						}
					} else {
						showSnackBar("Some error occurred. Please try again later.");
					}
				}
				if (mSeries.isEmpty()) {
					TextView textView = findViewById(R.id.no_points_textview_MainActivity);
					textView.setVisibility(View.VISIBLE);
				} else {
					TextView textView = findViewById(R.id.no_points_textview_MainActivity);
					textView.setVisibility(View.GONE);
				}
            }
        });
    }

    public void initFAB() {
        fab = findViewById(R.id.fab_MainActivity);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
				toggle();
			}
        });
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

    public void onPostRadioButtonChanged(int radioButtonIndex, final boolean shouldResetGraph) {
        setCurrentAxis(radioButtonIndex);
        preGraphReset();
        resetGraph(shouldResetGraph);
    }
    
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
            callExportData();
        }
        else if (id == R.id.button_logout_ActivityDrawer) {
            logout();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private void callExportData() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.download_alert_title)
				.setMessage(R.string.download_alert_message)
				.setIcon(getDrawable(R.drawable.ic_warning_black_24dp))
				.setPositiveButton(R.string.download_anyway, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Map<String, String> data = new HashMap<>();
						data.put("uid", getUserId());
						data.put("username", getUserName());
						FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();
						mFunctions.getHttpsCallable("exportData")
								.call(data)
								.addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
									@Override
									public void onSuccess(HttpsCallableResult httpsCallableResult) {
										Log.d(TAG, String.valueOf(httpsCallableResult.getData()));
										Log.i(TAG, String.valueOf(httpsCallableResult.getClass()));
										showSnackBar("Data exported to Firestore storage. Preparing to download.");
										try {
											downloadFile();
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								})
								.addOnFailureListener(new OnFailureListener() {
									@Override
									public void onFailure(@NonNull Exception e) {
										showSnackBar("File download failed");
										Log.e(TAG, e.getMessage());
									}
								});
					}
				})
				.setNegativeButton("Cancel", null)
				.show();
    }
	
	private void downloadFile() throws IOException {
		FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
		StorageReference storageReference = firebaseStorage.getReference();
		StorageReference exportDataRef = storageReference.child(getExportFileReference());
		
		exportDataRef.getFile(getLocalFile(getUserName() + ".json")).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
			@Override
			public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
				Log.i(TAG, "Total bytes count: " + taskSnapshot.getTotalByteCount());
				Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout_MainActivity), R.string.download_completed, Snackbar.LENGTH_LONG);
				snackbar.setAction(R.string.view_file, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						File file = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
						intent.setDataAndType(Uri.fromFile(file), "*/*");
						startActivity(intent);
					}
				});
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				showSnackBar("File download failed.");
				Log.e(TAG, "Exception occurred while downloading file.", e);
			}
		});
		
	}
	
	public void getPermissions() {
		if (ContextCompat.checkSelfPermission(MainActivity.this,
				android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			
			if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
					android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(
						MainActivity.this,
						new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
						1
				);
			}
		}
		Log.i("PERMISSIONS", "Storage permissions taken");
	}
	
	private File getLocalFile(String filename) throws IOException {
		createDownloadFolder();
		if (!isExternalStorageWritable()) {
			Log.i(TAG, "Permissions required.");
			getPermissions();
		}
		File file = new File(Environment.getExternalStoragePublicDirectory(
				getString(R.string.app_name)), filename);
		if (!file.createNewFile()) {
			Log.i(TAG, "Directories not created.");
		}
		return file;
	}
	
	private void createDownloadFolder() {
		File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));
		if (!mediaStorageDir.mkdirs()) {
			showSnackBar("Downloads folder not found.");
		}
	}
	
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.DIRECTORY_DOWNLOADS.equals(state);
	}
	
	
	private String getExportFileReference() {
		return "exportData/" + getUserId() + "/export.json";
	}
	
	private void logout() {
    	FirebaseAuth.getInstance().signOut();
    	startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }

    private void onMenuItemSelectedBehaviour(CompoundButton compoundButton, int index) {
        if (compoundButton.isChecked()) {
            showSnackBar(getString(R.string.atleast_one_axis_should_be_set));
            return;
        }
        for (CompoundButton cmpdButton: radioButtons) cmpdButton.setChecked(false);
        radioButtons[index].setChecked(true);
    }
	
	private String getCurrentSensor() {
    	return "Accelerometer";
	}
	
	private void resetGraph(boolean shouldResetGraph) {
        Log.i(TAG, "Reset graph: " + shouldResetGraph);
        if (shouldResetGraph) {
            Log.i(TAG, "Loading data for axis: " + getCurrentAxis());
            getAccInOrder.limit(1000).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult() != null && task.isSuccessful()) {
                            int index = 0;
                            Log.d(TAG, "Size of data recieved:" + task.getResult().size());
                            final DataPoint[] points = new DataPoint[1000];
                            long prevX = -1, currX;
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                currX = (long)doc.get(getString(R.string.order_by_parameter));
                                if (prevX < currX) {
                                    DataPoint dataPoint = new DataPoint(currX, (double) doc.get(getCurrentAxis()));
                                    points[index] = dataPoint;
                                    index++;
                                    prevX = currX;
                                } else {
                                    Log.e(TAG, "X values not in ascending order," + prevX + " > " + currX);
                                }
                            }
                            if (index > 0) {
                                final int finalIndex = index;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSeries.resetData(Arrays.copyOfRange(points, 0, finalIndex));
                                    }
                                }).start();
                            }

                        }
                        else {
                            Log.e(TAG, "Complete database GET FAILED", task.getException());
                        }
                    }
                }).continueWith(new Continuation<QuerySnapshot, Object>() {
                    @Override
                    public Object then(@NonNull Task<QuerySnapshot> task) {
                        postGraphReset();
                        return null;
                    }
                });
        }
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
                    }
                }
            });
        }
    }

    private void postGraphReset() {
		graphTitle.setText(getGraphTitle());
//		graphView.setTitle(getGraphTitle());
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void preGraphReset() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setRadioButtonDefaultChecked(NavigationView navigationView) {
        MenuItem menuItem = navigationView.getMenu().findItem(R.id.radio_buttonX_axis);
        RadioButton radioButton = (RadioButton) menuItem.getActionView();
        radioButton.setChecked(true);
    }

    private void toggle() {
        if(isMyServiceRunning(CollectDataService.class)) {
        	onPostService();
        }
        else {
        	onPreService();
        }
    }
	
	private void startMyService() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(collectDataIntent);
		} else {
			startService(collectDataIntent);
		}
		showSnackBar(getString(R.string.service_started_message));
	}
	
	private void startAlertSequence() {
    	new AlertDialog.Builder(this)
				.setTitle(getString(R.string.orientation_alert_title))
				.setMessage(getString(R.string.orientation_alert_message))
				.setPositiveButton(getString(R.string.orientation_alert_positive_button_message), null)
				.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_phone_android_black_24dp))
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						Log.d(TAG, "Alert 1 onDismiss called.");
						showStartWalkingAlertAndStartService();
					}
				})
				.show();
 	}
 	
 	private void onPreService() {
		startAlertSequence(); // Takes care of alerts, and whether to change fab icon or not
	}
	
	private void onPostService() {
		stopService(collectDataIntent);
		Log.d(TAG, "Service Stopped, user interrupt");
		showSnackBar(getString(R.string.service_stopped_message));
		fab.setImageDrawable(getDrawable(R.drawable.ic_cloud_on_white_24dp));
	}
	
	private void showStartWalkingAlertAndStartService() {
		final AlertDialog.Builder startWalkingAlertBuilder = new AlertDialog.Builder(this)
				.setTitle(getString(R.string.start_walking_alert_title))
				.setCancelable(false)
				.setNegativeButton(getString(R.string.start_walking_alert_negative_button_message), null)
				.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_directions_run_24px))
				.setMessage(""); // Had to set something, else dynamic changes won't display
		final AlertDialog alertDialog = startWalkingAlertBuilder.create();
		alertDialog.show();
		int countDownTime = 16; // TODO: make 16 before publishing
		new CountDownTimer(1000 * countDownTime, 1000) {
			@Override
			public void onTick(long millisUntilFinished) {
				if (!alertDialog.isShowing())
					return;
//				Log.d(TAG, "millisUntilFinished: " + millisUntilFinished);
				alertDialog.setMessage(getString(R.string.start_walking_alert_message, (int)(millisUntilFinished / 1000)));
			}
			
			@Override
			public void onFinish() {
				if (alertDialog.isShowing()) {
					alertDialog.dismiss();
					startMyService();
					fab.setImageDrawable(getDrawable(R.drawable.ic_cloud_off_white_24dp));
				}
			}
		}.start();
	}
	
	private String getGraphTitle() {
		if (getCurrentSensor().equals("Accelerometer")) {
			switch (getCurrentAxis()) {
				case "x_axis":
					return getString(R.string.acc_x);
				case "y_axis":
					return getString(R.string.acc_y);
				default:
					return getString(R.string.acc_z);
			}
		}
		else {
			switch (getCurrentAxis()) {
				case "x_axis":
					return getString(R.string.gyro_x);
				case "y_axis":
					return getString(R.string.gyro_y);
				default:
					return getString(R.string.gyro_z);
			}
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
    }

    private String getCurrentAxis() {
        return currentAxis;
    }
    
    private String getUserId() {
        return FirebaseAuth.getInstance().getUid();
    }
    
    private String getUserName() {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user != null) {
			return FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
		}
		return "Google User";
	}
    
    public void showSnackBar(String message) {
    	Snackbar.make(findViewById(R.id.coordinatorLayout_MainActivity), message, Snackbar.LENGTH_LONG).show();
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    /*void startAnim(){
        avi.show();
        // or avi.smoothToShow();
    }

    void stopAnim(){
        avi.hide();
        // or avi.smoothToHide();
    }*/
}