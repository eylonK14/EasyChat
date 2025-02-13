
package com.example.lenovo.eazyproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;  // Request code for Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    EditText inputMail, inputPass;
    Button btnLogin, btnSignUp, btnLogout;
    SignInButton btnGoogleSignIn;
    private FirebaseFirestore db;

    String name;

    TextView helloTxt;

    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Log.d(TAG, "ActivityResult: Google Sign-In result received");
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                Log.d(TAG, "ActivityResult: Google Sign-In successful, ID: " + account.getId());
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                Log.e(TAG, "ActivityResult: Google Sign-In failed", e);
                                Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // השמת ערכים למשתנים בהתאם לכל יישום בlayout (כפתור, שדה הזנה וכו'...)

        inputMail = findViewById(R.id.inputMailLogin);
        inputPass = findViewById(R.id.inputPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogout = findViewById(R.id.btnLogout);
        btnGoogleSignIn = (SignInButton) findViewById(R.id.btnGoogleSignIn); // Google Sign-In Button
        helloTxt = findViewById(R.id.helloText);


        btnLogin.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle()); // New OnClickListener for Google


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        } else {
            Log.e(TAG, "onCreate: btnGoogleSignIn is NULL. Check activity_login.xml");
        }

        // Initialize Google Sign-In
        Log.d(TAG, "onCreate: Configuring Google Sign-In");
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Web client ID from Firebase
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // Check if user is already logged in
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null)
        {
            Log.d(TAG, "onCreate: User already signed in: " + user.getEmail());
            String id = user.getUid();
            btnLogout.setVisibility(View.VISIBLE);
            getUserDataFB(id);
        }
        Log.d(TAG, "onCreate: Google Sign-In Initialized Successfully");

    }

    private void signInWithGoogle() {
        Log.d(TAG, "signInWithGoogle: Starting Google Sign-In Intent");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activityResultLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: Authenticating with Firebase");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "firebaseAuthWithGoogle: Firebase Authentication successful, User: " + user.getEmail());
                        String id = user.getUid();
                        getUserDataFB(id);
                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "firebaseAuthWithGoogle: Firebase Authentication failed", exception);
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @SuppressLint("SetTextI18n")
    @Override
    /*
     * פונקיצה זו תפעל במידה והמשתמש לחץ על כפתור ההתחברות (Login).
     * פונקציה זו מחפשת בתוך הקבצים בבסיס הנתונים את שם המשתמש שהוכנס, ובודקת האם הוא קיים.
     * במידה והוא קיים, הסיסמה שהכונסה תיבדק והמשתמש יועבר לאקטיביטי המתאים לו על פי סוג המשתמש (הורה או ילד). אם הוא לא המערכת תתריע על כך למשתמש.
     * במידה וכפתור ההרשמות (SignUp) נלחץ, המשתמש יועבר לאקטיביטי ההירשמות.
     *  */
    public void onClick(View v) {
        if(v==btnLogin) //אם נלחץ כפתור לוגאין
        {


            String mail = inputMail.getText().toString();
            String password = inputPass.getText().toString();

            if(mail.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "נא להכניס ערכים לשדות", Toast.LENGTH_SHORT).show();
            }else {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(mail, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){

                            btnLogout.setVisibility(View.VISIBLE);

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            assert user != null;
                            String id = user.getUid();

                            getUserDataFB(id);

                        }
                    }
                });
            }

        }
        else if(v==btnSignUp)
        {
            openSignUp();
        }
        else if(v == btnLogout)
        {
            FirebaseAuth.getInstance().signOut();
            btnLogout.setVisibility(View.INVISIBLE);
            helloTxt.setText("Hello, Guest");
        }
    }

    // פונקציה זו תפתח את האקטיביטי המרכזי של הילד
    public void openMain(String roomName) // במידה והמשתמש כבר רשום ורק מעוניין להתחבר, יועבר לדף הבית
    {
        Intent in = new Intent(this ,MainActivity.class);
        in.putExtra("roomName", roomName);
        startActivity(in);
    }

    // פונקציה זו תפתח את האקטיביטי המרכזי של ההורה
    public void openMainPar(String roomName) // במידה והמשתמש כבר רשום ורק מעוניין להתחבר, יועבר לדף הבית
    {
        Intent in = new Intent(this ,MainActivityPar.class);
        in.putExtra("roomName", roomName);
        startActivity(in);
    }

    // פונקציה זו תפתח את האקטיביטי של ההרשמות
    public void openSignUp()
    {
        Intent in = new Intent(this, SignUpActivity.class);
        startActivity(in);
    }
    public void getUserDataFB (String id) {
        DocumentReference ref = db.collection("users").document(id);
        ref.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Toast.makeText(LoginActivity.this, "A problem occurred", Toast.LENGTH_SHORT).show();
                }

                if(value.exists()){

                    name = value.getString("username");
                    helloTxt.setText("Hello, " + name);

                    boolean isChild = value.getBoolean("isChild");

                    if (isChild){
                        openMain(value.getString("username"));
                    } else {
                        getChildFromDB(value.getString("childID"), value.getString("username"));
                    }
                }

            }
        });
    }

    public void getChildFromDB(String childId, final String parName){
        DocumentReference ref = db.collection("users").document(childId);
        ref.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Toast.makeText(LoginActivity.this, "A problem occurred", Toast.LENGTH_SHORT).show();
                }

                assert value != null;
                if(value.exists()){

                    name = parName;
                    helloTxt.setText("Hello, " + name);

                    openMainPar(value.getString("username"));
                }

            }
        });
    }

}