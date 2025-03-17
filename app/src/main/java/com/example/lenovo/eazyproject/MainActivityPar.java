package com.example.lenovo.eazyproject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivityPar extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private Button btnSignOut;
    private static final String TAG = "MainActivityPar";

    public String roomName;
    public List<String> questions = new ArrayList<>();
    boolean isActivityOn = false;
    AlertDialog logoutD;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isActivityOn = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_par);

        mAuth = FirebaseAuth.getInstance();
        btnSignOut = findViewById(R.id.btnSignOut);

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut(); // Logs out the user
            startActivity(new Intent(MainActivityPar.this, LoginActivity.class)); // Go back to login
            finish(); // Close current activity
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up navigation drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize room name
        Bundle extras = this.getIntent().getExtras();
        if (extras != null){
            roomName = extras.getString("roomName");
        } else {
            Log.e(TAG, "Room name is null. Cannot proceed.");
            return;
        }

        db = FirebaseFirestore.getInstance();

        // Fetch questions from Firestore
        db.collection("ques").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int count = 0;
                for (QueryDocumentSnapshot document : task.getResult()) {
                    count++;
                    String questionText = document.getString("text");
                    questions.add(questionText);

                    // Add questions to navigation menu
                    @SuppressLint("CutPasteId") NavigationView mLeftDrawer = findViewById(R.id.nav_view);
                    Menu menu = mLeftDrawer.getMenu();
                    menu.add(1, count, count, questionText);
                }
            } else {
                Log.w(TAG, "Error fetching questions.", task.getException());
            }
        });

        // Listen for messages in the current room and display the full history
        db.collection("messages")
                .whereEqualTo("room", roomName)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to messages", e);
                        return;
                    }
                    if (snapshots != null) {
                        Log.d(TAG, "Firestore returned " + snapshots.size() + " messages for room: " + roomName);
                        runOnUiThread(() -> {
                            LinearLayout chatLayout = findViewById(R.id.chatPar);
                            if (chatLayout == null) {
                                Log.e(TAG, "chatPar layout not found.");
                                return;
                            }
                            chatLayout.removeAllViews();
                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                String message = document.getString("text");
                                // Check the sender field to determine alignment
                                String sender = document.getString("sender");
                                boolean isSentByUser = sender != null && sender.equals("parent");
                                if (message != null) {
                                    Log.d(TAG, "Adding message: " + message + " (sender: " + sender + ")");
                                    addMessageToView(message, isSentByUser);
                                } else {
                                    Log.w(TAG, "Document " + document.getId() + " is missing the 'text' field.");
                                }
                            }
                        });
                    } else {
                        Log.w(TAG, "Snapshots is null for room: " + roomName);
                    }
                });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_par, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        sendMessage("ques" + id); // Send selected question
        Log.d(TAG, "Question sent: ques" + id);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Adds a message to the chat view.
     * Parent's messages will be displayed on the left and child's on the right.
     */
    public void addMessageToView(String message, boolean isSentByUser) {
        runOnUiThread(() -> {
            LinearLayout chatLayout = findViewById(R.id.chatPar);
            if (chatLayout == null) {
                Log.e(TAG, "Chat layout not found.");
                return;
            }

            TextView messageView = new TextView(this);
            messageView.setText(message);
            messageView.setTextSize(16);
            messageView.setPadding(10, 10, 10, 10);

            // For parent's messages (isSentByUser == true), align left.
            // For child's messages, align right.
            if (isSentByUser) {
                // Parent's message: left-aligned with green background.
                messageView.setBackgroundResource(R.drawable.sent_message_background);
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            } else {
                // Child's message: right-aligned with white (or default) background.
                messageView.setBackgroundResource(R.drawable.received_message_background);
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            }

            // Create layout parameters and set a bottom margin to add space between messages
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int marginBottom = (int) (8 * getResources().getDisplayMetrics().density); // 8dp bottom margin
            layoutParams.setMargins(0, 0, 0, marginBottom);
            messageView.setLayoutParams(layoutParams);

            chatLayout.addView(messageView);
        });
    }

    /**
     * Sends a message to the Firestore database.
     * A "sender" field is added to distinguish between parent and child messages.
     */
    public void sendMessage(String message) {
        if (roomName == null || roomName.isEmpty()) {
            Log.e(TAG, "Room name is null or empty. Cannot send message.");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", message);
        messageData.put("room", roomName);
        // Add the sender field so we can differentiate messages later
        messageData.put("sender", "parent");
        messageData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("messages").add(messageData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Message sent: " + message))
                .addOnFailureListener(e -> Log.e(TAG, "Error sending message", e));
    }

    public void onSend(View view) {
        EditText newMess = findViewById(R.id.newMess);
        String newMessage = newMess.getText().toString();

        if (!newMessage.isEmpty()) {
            sendMessage(newMessage);
            // When sending locally, mark as parent's message.
            addMessageToView(newMessage, true);
        }
        newMess.setText(""); // Clear input field
    }

    @Override
    protected void onPause() {
        isActivityOn = false;
        if (logoutD != null && logoutD.isShowing()) {
            logoutD.dismiss();
        }
        super.onPause();
    }

    public void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void onMessage(String msg) {
        // No additional handling for now.
    }
}
