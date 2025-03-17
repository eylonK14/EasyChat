
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

import android.widget.RadioButton;

import android.widget.TableRow;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.gms.common.SignInButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;



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
                        Log.d(TAG, "ActivityResult: Google Sign-In result received, resultCode=" + result.getResultCode());

                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Log.d(TAG, "ActivityResult: Google Sign-In SUCCESS, processing result");
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                Log.d(TAG, "ActivityResult: Google Sign-In successful, ID: " + account.getId());
                                firebaseAuthWithGoogle(account.getIdToken());
                            } catch (ApiException e) {
                                Log.e(TAG, "ActivityResult: Google Sign-In FAILED", e);
                                Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "ActivityResult: Google Sign-In failed, resultCode=" + result.getResultCode());
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
            handleUserRole(id);
        }
        Log.d(TAG, "onCreate: Google Sign-In Initialized Successfully");

    }

    private void signInWithGoogle() {
        Log.d(TAG, "signInWithGoogle: Signing out to force account selection");

        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d(TAG, "signInWithGoogle: Signed out successfully, launching sign-in intent");

            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });
    }


    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: Authenticating with Firebase, ID Token: " + (idToken != null ? "Received" : "NULL"));

        if (idToken == null) {
            Log.e(TAG, "firebaseAuthWithGoogle: ERROR - ID Token is NULL");
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "firebaseAuthWithGoogle: Firebase Authentication successful, User: " + (user != null ? user.getEmail() : "NULL"));


                        if (user != null) {
                            checkIfUserExists(user.getUid(), user.getEmail());
                        }

                    } else {
                        Exception exception = task.getException();
                        Log.e(TAG, "firebaseAuthWithGoogle: Firebase Authentication failed", exception);
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfUserExists(String userId, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d(TAG, "checkIfUserExists: User found in Firestore, ID: " + userId);
                    // User already exists → Check role and redirect
                    handleUserRole(userId);
                } else {
                    Log.e(TAG, "checkIfUserExists: User NOT found in Firestore. Asking for role selection...");
                    // New user → Ask if Parent or Child
                    showRoleSelectionDialog(userId, email);
                }
            } else {
                Log.e(TAG, "Firestore Error: ", task.getException());
            }
        });
    }

    private void showRoleSelectionDialog(String userId, String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Your Role");

        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_role_selection, null);
        builder.setView(dialogView);

        // Initialize elements inside the dialog
        RadioButton rbParent = dialogView.findViewById(R.id.rbParent);
        RadioButton rbChild = dialogView.findViewById(R.id.rbChild);
        EditText etChildEmail = dialogView.findViewById(R.id.etChildEmail);
        TableRow childEmailRow = dialogView.findViewById(R.id.childEmailRow);

        // Show child email field only if "Parent" is selected
        rbParent.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                childEmailRow.setVisibility(View.VISIBLE);
            }
        });

        rbChild.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                childEmailRow.setVisibility(View.GONE);
            }
        });

        builder.setPositiveButton("Continue", (dialog, which) -> {
            String role;
            if (rbParent.isChecked()) {
                role = "Parent";
            } else if (rbChild.isChecked()) {
                role = "Child";
            } else {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }


            String childEmail = "";
            if (role.equals("Parent")) {
                childEmail = etChildEmail.getText().toString().trim();
                if (childEmail.isEmpty() || !isEmailValid(childEmail)) {
                    Toast.makeText(this, "Enter a valid child email", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            saveUserToFirestore(userId, email, role, childEmail);
        });

        builder.setCancelable(false);
        builder.show();
    }

    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }




    private void saveUserToFirestore(String userId, String email, String role, String childEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("mail", email);
        user.put("role", role);

        if (role.equals("Parent")) {
            Log.d(TAG, "Searching for child with email: " + childEmail);

            db.collection("users").whereEqualTo("mail", childEmail).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    String childUid = task.getResult().getDocuments().get(0).getId();
                    user.put("childID", childUid);

                    db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Parent linked to child successfully: " + childUid);
                        startActivity(new Intent(LoginActivity.this, MainActivityPar.class));
                        finish();
                    }).addOnFailureListener(e -> Log.e(TAG, "Error saving parent to Firestore", e));
                } else {
                    Log.e(TAG, "Child email not found in Firestore.");
                    Toast.makeText(this, "Child email not found. Please have the child sign up first.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Child account created successfully.");
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }).addOnFailureListener(e -> Log.e(TAG, "Error saving child to Firestore", e));
        }
    }




    private void saveUserToDB(String userId, Map<String, Object> user, String role) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "User added to Firestore with role: " + role);

            // Redirect user based on role
            if ("Parent".equals(role)) {
                startActivity(new Intent(LoginActivity.this, MainActivityPar.class));
            } else {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
            finish();
        }).addOnFailureListener(e -> Log.e(TAG, "Error adding user to Firestore", e));
    }


    private void handleUserRole(String userId) {
        Log.d(TAG, "handleUserRole: Checking Firestore for role, userId: " + userId);
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();
                String role = document.getString("role");
                Log.d(TAG, "handleUserRole: Role found: " + role);
                if ("Child".equals(role)) {
                    Log.d(TAG, "handleUserRole: Redirecting to Child Activity...");
                    openMain(document.getString("username"));  // Redirect to child activity
                } else if ("Parent".equals(role)) {
                    String childId = document.getString("childID");

                    if (childId != null && !childId.isEmpty()) {
                        Log.d(TAG, "handleUserRole: Parent has child, getting child data...");
                        getChildFromDB(childId, document.getString("username"));
                    } else {
                        Log.d(TAG, "handleUserRole: Parent has NO child, opening Parent Activity...");
                        openMainPar(document.getString("username")); // If no child assigned yet, just open parent view
                    }
                }
            } else {
                Log.e(TAG, "Firestore Error: ", task.getException());
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

                            handleUserRole(id);

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


    public void getChildFromDB(String childId, final String parName) {
        Log.d(TAG, "getChildFromDB: Fetching child data for childId: " + childId);

        DocumentReference ref = db.collection("users").document(childId);
        ref.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "getChildFromDB: Error fetching child data", error);
                    Toast.makeText(LoginActivity.this, "A problem occurred while fetching child data", Toast.LENGTH_SHORT).show();
                    return;  // Prevents further execution if there's an error
                }

                if (value == null || !value.exists()) {
                    Log.e(TAG, "getChildFromDB: No child found in Firestore for ID: " + childId);
                    Toast.makeText(LoginActivity.this, "Child not found. Please check Firestore.", Toast.LENGTH_LONG).show();
                    return;  //  Prevents further execution if child doesn't exist
                }

                Log.d(TAG, "getChildFromDB: Child data found! Redirecting Parent to MainActivityPar");

                name = parName;
                helloTxt.setText("Hello, " + name);

                String childUsername = value.contains("username") ? value.getString("username") : "Unknown";
                Log.d(TAG, "getChildFromDB: Opening MainActivityPar with child username: " + childUsername);

                Intent intent = new Intent(LoginActivity.this, MainActivityPar.class);
                intent.putExtra("roomName", childUsername);
                startActivity(intent);
                finish();
            }
        });
    }


}