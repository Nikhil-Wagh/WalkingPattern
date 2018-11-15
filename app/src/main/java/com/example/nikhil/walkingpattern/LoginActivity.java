package com.example.nikhil.walkingpattern;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "OnCreate");

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestProfile()
                .requestScopes(new Scope(Scopes.PROFILE))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        mAuth = FirebaseAuth.getInstance();

        SignInButton signInButton = findViewById(R.id.button_google_sign_in_LoginActivity);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "OnStart");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void signIn() {
        Log.d(TAG, "signIn");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                assert account != null;
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

        AuthCredential authCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(authCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                        }
                        else {
                            showSnackBar("Login failed");
                        }
                    }
                }).continueWith(new Continuation<AuthResult, Object>() {
                    @Override
                    public Object then(@NonNull Task<AuthResult> task) throws Exception {
                        if (task.isSuccessful()) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
							FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
									.setTimestampsInSnapshotsEnabled(true)
									.build();
							db.setFirestoreSettings(settings);
                            getNewUserSnapshot(db);
                            updateUI(FirebaseAuth.getInstance().getCurrentUser());
                            /*db.collection("AppData")
                                    .add(UserSnapshot)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            updateUI(user);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            showSnackBar("User Login Failed");
                                        }
                                    });*/
                        }
                        return null;
                    }
        });

    }
    
    private void getNewUserSnapshot(FirebaseFirestore db) {
        try {
            Map<String, Integer> map = new HashMap<>();
            map.put("accelerometer_readings_count", 0);
            DocumentReference document = db.collection("AppData").document(mAuth.getCurrentUser().getUid());
            document.collection("AccelerometerReadings").add(map)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.i(TAG, "AccelerometerReadings Collection created successfully");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showSnackBar("AccelerometerReadings collection could not be created.");
                            Log.e(TAG, "AccelerometerReadings collection could not be created.", e);
                        }
                    });

            map.clear();
            map.put("gyroscope_readings_count", 0);
            document.collection("GyroscopeReadings").add(map)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.i(TAG, "GyroscopeReadings collection created successfully");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showSnackBar("GyroscopeReadings collection could not be created");
                            Log.e(TAG, "GyroscopeReadings collection could not be created", e);
                        }
                    });

        } catch (NullPointerException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "User Login Successfull", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private void showSnackBar(String message) {
        Snackbar.make(findViewById(R.id.coordinator_layout_LoginActivity), message, Snackbar.LENGTH_LONG).show();
    }

}
