package com.example.lenovo.eazyproject;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableRow;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SignUpActivity";
    EditText etU, etM, etP, etP2, etMC;
    RadioButton cbChild, cbParent;
    TableRow tr;
    Button clear;
    RadioGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etU = findViewById(R.id.inputMailLogin);
        etM = findViewById(R.id.inputMail);
        etP = findViewById(R.id.inputPass);
        etP2 = findViewById(R.id.inputPass2);
        etMC = findViewById(R.id.inputChildMail);
        cbChild = (RadioButton) findViewById(R.id.child);
        cbParent = (RadioButton) findViewById(R.id.parent);
        tr = findViewById(R.id.childMTR);
        clear = findViewById(R.id.btnClear);

        group = findViewById(R.id.radio);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == cbParent.getId()){
                    tr.setVisibility(View.VISIBLE);
                } else{
                    tr.setVisibility(View.INVISIBLE);
                }
            }
        });


        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etU.setText("");
                etM.setText("");
                etP.setText("");
                etP2.setText("");
                etMC.setText("");

                group.clearCheck();

            }
        });

    }


    // פונקציה זו פותחת את המיין (והאקטיביטי) של הילד
    public void openMain() {
        Intent in = new Intent(this ,MainActivity.class); //הגדרת הintent כך שיפעיל את האקטיביטי של הילד במידה ויפעילו אותו
        startActivity(in); //הפעלת הintent
    }

    // פונקציה זו פותחת את המיין (והאקטיביטי) של ההורה
    public void openMainPar() {
        Intent in = new Intent(this ,MainActivityPar.class); //הגדרת הintent כך שיפעיל את האקטיביטי של ההורה במידה ויפעילו אותו
        startActivity(in); //הפעלת הintent
    }
//    public void openSSignUp2() {
//        Intent in = new Intent(this ,SignUp2Activity.class);
//        startActivity(in);
//    }


    /*
    פונקציה זו אחראית על יצירת המשתמש - ברגע אשר כפתור הsubmit נלחץ, הפונציקה תקבל את המידע שהוכנס בכל אחד מהשדות,
     תיצור משתמש חדש (או תתריע בהתאם אם יש בעיות עם המידע). לאחר מכן תעביר הפונקציה את המשתמש לאקטיביטי המתאים לו
    */

    public void onClickSubmit(View view) {

        int countMistakes=0;

        Log.d(TAG, "onClickSubmit: function started");

        //username
        final String username = etU.getText().toString(); // קבלת המידע משדה שם המשתמש והפיכתו למחרוזת
        if (username.equals("")) { // במידה ולא הוכנס שם משתמש יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this, "חובה להזין שם משתמש", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        else if(username.length() <= 4) { // במידה ושם המשתמש קצר מדי יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this, "שם המשתמש חייב להכיל 5 תווים לפחות", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        Log.d(TAG, "onClickSubmit: username success");

        //mail
        final String mail = etM.getText().toString(); // קבלת המידע משדה המייל והפיכתו למחרוזת
        if (!isEmailValid(mail)) { // במידה והמייל אינו חוקי יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this, "כתובת מייל אינה חוקית", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        Log.d(TAG, "onClickSubmit: mail success");

        //password
        final String password = etP.getText().toString(); // קבלת המידע משדה הסיסמה והפיכתו למחרוזת
        if (password.equals("")){ // במידה ולא הוכנסה סיסמה יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this,"חובה להזין סיסמא", Toast.LENGTH_LONG).show();
            countMistakes++;
        }
        else if(password.length() <= 4) { // במידה והסיסמה קצרה מדי יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this, "סיסמא חייבת להכיל 5 תווים לפחות", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        Log.d(TAG, "onClickSubmit: password success");

        //password2
        final String password2 = etP2.getText().toString(); // קבלת מידע משדה אימות הסיסמה והפיכתו לטקסט

        //is password 2 equals password 1
        if(!password.equals(password2)) { // במידה והסיסמאות לא  תואמות יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this, "הסיסמאות לא תואמות", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        Log.d(TAG, "onClickSubmit: match password success");

        //child or parent

        RadioButton cbParent = (RadioButton) findViewById(R.id.parent); // קבלת הסימון מתוך שני התיבות לסימון - האם המשתמש הוא הורה או ילד
        if(!cbChild.isChecked() && !cbParent.isChecked()){ // במידה ושני התיבות אינן מסומנות
            Toast.makeText(this, "יש למלא האם אתה ילד או הורה", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        //child mail
        final String childMail = etMC.getText().toString(); // קבלת המידע משדה המייל והפיכתו למחרוזת
        if (!isEmailValid(childMail) && cbParent.isChecked()) { // במידה והמייל אינו חוקי יוקפץ למסך toast שיתריע על כך
            Toast.makeText(this, "כתובת מייל אינה חוקית", Toast.LENGTH_LONG).show();
            countMistakes++;
        }

        Log.d(TAG, "onClickSubmit: child mail success");

        Log.d(TAG, "onClickSubmit: check success - " + countMistakes);


        if(countMistakes == 0)
        {
            if (cbParent.isChecked()){
                final FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference cr = db.collection("users");
                Query query = cr.whereEqualTo("mail", childMail); // מביא משתמש ששמו זהה לשם שהוזן
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) { // אם אכן היה משתמש כזה
                                final QueryDocumentSnapshot document = task.getResult().iterator().next();//מביא את הראשון והיחיד - לא יכולים להיות שניים כאלו
                                String isChildFromDB = document.get("isChild").toString();
                                boolean isChild = Boolean.valueOf(isChildFromDB);
                                if (isChild){
                                    String id = document.getId(); //Warning!
                                    Log.d(TAG, "onComplete: id - " + id);
                                    Log.d(TAG, "onComplete: isChild - " + isChild);
                                    saveUserToMB(mail, password2, username, id);
                                }
                            }
                            Log.w("", "Error getting documents.", task.getException());
                        }
                    }
                });
            } else{
                saveUserToMB(mail, password2, username, "");
            }
        }

    }


    @Override
    public void onClick(View v) {
    }

    public static boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public void  saveUserToMB(final String mail, String password, final String username, final String childId)
    {
        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        firebaseAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                    String id = firebaseUser.getUid();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    DocumentReference documentReference = db.collection("users").document(id);

                    Map<String, Object> userData = new HashMap<>();

                    if(!cbChild.isChecked()){
                        userData.put("childID", childId);
                    }

                    userData.put("isChild", cbChild.isChecked());
                    userData.put("mail", mail);
                    userData.put("username", username);

                    documentReference.set(userData).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(SignUpActivity.this, "המשתמש נשמר בהצלחה", Toast.LENGTH_SHORT).show();

                                if(cbChild.isChecked()) // במידה והמשתמש סימן שהוא ילד - הוא יועבר לאקטיביטי של הילד
                                {
                                    openMain();
                                }
                                else if(cbParent.isChecked()) // במידה והמשתמש סימן שהוא הורה - הוא יועבר לאקטיביטי של ההורה
                                {
                                    openMainPar();
                                }
                        }
                    });
                }
            }
        });
    }
}
