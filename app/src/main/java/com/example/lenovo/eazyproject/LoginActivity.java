
package com.example.lenovo.eazyproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText inputMail, inputPass;
    Button btnLogin, btnSignUp, btnLogout;
    private FirebaseFirestore db;

    String name;

    TextView helloTxt;

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
        btnLogin.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
        btnLogout.setOnClickListener(this);


        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        helloTxt = findViewById(R.id.helloText);

        if (user != null)
        {
            String id = user.getUid();
            btnLogout.setVisibility(View.VISIBLE);

            getUserDataFB(id);
        }
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