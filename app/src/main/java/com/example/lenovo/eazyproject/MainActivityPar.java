package com.example.lenovo.eazyproject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
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

        // Setup Toolbar and Navigation Drawer as before...
        // (Assuming this code remains unchanged.)

        // Retrieve room name from extras
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            roomName = extras.getString("roomName");
        } else {
            Log.e(TAG, "Room name is null. Cannot proceed.");
            return;
        }

        // Initialize Firestore instance and set settings (ideally do this only once in your Application class)
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Fetch questions from Firestore and add them to the Navigation Menu
        db.collection("ques").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int count = 0;
                NavigationView navView = findViewById(R.id.nav_view);
                Menu menu = navView.getMenu();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    count++;
                    String questionText = document.getString("text");
                    questions.add(questionText);
                    // Add each question as a menu item (id = count)
                    menu.add(1, count, count, questionText);
                }
                // Finally, add a fixed "Add New Question" menu item with id 9999.
                menu.add(1, 9999, 9999, "Add New Question");
            } else {
                Log.w(TAG, "Error fetching questions.", task.getException());
            }
        });

        // Set up NavigationView listener
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Listen for messages in the current room (existing code)
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
                                String sender = document.getString("sender");
                                // For MainActivityPar, assume parent's messages are sent with "parent"
                                boolean isSentByUser = (sender != null && sender.equals("parent"));
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
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 9999) {
            // "Add New Question" menu item selected.
            showAddQuestionDialog();
        } else {
            // Existing behavior: send the selected question.
            sendMessage("ques" + id); // Send selected question command
            Log.d(TAG, "Question sent: ques" + id);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void sendMessage(String message) {
        if (roomName == null || roomName.isEmpty()) {
            Log.e(TAG, "Room name is null or empty. Cannot send message.");
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", message);
        messageData.put("room", roomName);
        messageData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("messages").add(messageData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Message sent: " + message))
                .addOnFailureListener(e -> Log.e(TAG, "Error sending message", e));
    }

    // Method to show the dialog for adding a new question.
    private void showAddQuestionDialog() {
        // Inflate custom view (ensure you have dialog_new_question.xml in your layout resources)
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_question, null);
        EditText etQuestion = dialogView.findViewById(R.id.etQuestionText);
        EditText etAnswer1 = dialogView.findViewById(R.id.etAnswer1);
        EditText etAnswer2 = dialogView.findViewById(R.id.etAnswer2);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Question")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String questionText = etQuestion.getText().toString().trim();
                    String answer1 = etAnswer1.getText().toString().trim();
                    String answer2 = etAnswer2.getText().toString().trim();

                    if (!questionText.isEmpty() && !answer1.isEmpty() && !answer2.isEmpty()) {
                        // Create a map with question details.
                        Map<String, Object> newQuestion = new HashMap<>();
                        newQuestion.put("text", questionText);
                        newQuestion.put("ans1", answer1);
                        newQuestion.put("ans2", answer2);

                        // Add the new question to the "ques" collection.
                        db.collection("ques").add(newQuestion)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "New question added with ID: " + documentReference.getId());
                                    // Optionally, update the navigation menu by adding this new question item.
                                    NavigationView navView = findViewById(R.id.nav_view);
                                    Menu menu = navView.getMenu();
                                    // Insert the new item before the "Add New Question" item (assuming order 9999).
                                    int newId = documentReference.getId().hashCode();
                                    menu.add(1, newId, 998, questionText);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error adding new question", e));
                    } else {
                        Log.e(TAG, "All fields must be filled out.");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Handles received Firebase messages (for questions and chat messages).
     */
    public void onMessage(String message) {
        if (message == null || message.isEmpty()) {
            Log.e(TAG, "Received an empty or null message.");
            return;
        }
        if (message.startsWith("ques?")) {
            String questionId = message.substring(5).trim();
            handleQuestionMessage(questionId);
        } else {
            runOnUiThread(() -> {
                LinearLayout chatLayout = findViewById(R.id.chatPar);
                if (chatLayout == null) {
                    Log.e(TAG, "Chat layout not found in activity_main_par.xml.");
                    return;
                }
                TextView messageView = new TextView(getApplicationContext());
                messageView.setText(message);
                chatLayout.addView(messageView);
            });
        }
    }

    /**
     * Handles question messages by displaying a dialog with possible answers.
     */
    public void handleQuestionMessage(String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            Log.e(TAG, "Question ID is null or empty.");
            return;
        }
        DocumentReference questionRef = db.collection("ques").document(questionId);
        questionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot value = task.getResult();
                if (value != null && value.exists()) {
                    runOnUiThread(() -> {
                        final Dialog dialog = new Dialog(MainActivityPar.this, R.style.Dialog);
                        dialog.setContentView(R.layout.custom); // Load custom dialog layout
                        String questionText = value.getString("text");
                        if (questionText == null) {
                            Log.e(TAG, "Question text is null in Firestore document.");
                            return;
                        }
                        dialog.setTitle(questionText + "?");

                        String[] answers = new String[4];
                        ImageButton[] answerButtons = new ImageButton[4];
                        answerButtons[0] = dialog.findViewById(R.id.imgBtn1);
                        answerButtons[1] = dialog.findViewById(R.id.imgBtn2);
                        answerButtons[2] = dialog.findViewById(R.id.imgBtn3);
                        answerButtons[3] = dialog.findViewById(R.id.imgBtn4);

                        for (int i = 1; i <= 4; i++) {
                            if (value.get("ans" + i) != null) {
                                answers[i - 1] = value.getString("ans" + i);
                                int imgResource = helperMethods.getImgId(questionId, answers[i - 1]);
                                answerButtons[i - 1].setImageResource(imgResource);
                                String selectedAnswer = answers[i - 1];
                                answerButtons[i - 1].setOnClickListener(v -> {
                                    helperMethods.sendMsg(selectedAnswer);
                                    dialog.dismiss();
                                });
                            } else {
                                answerButtons[i - 1].setVisibility(View.INVISIBLE);
                            }
                        }
                        dialog.show();
                    });
                } else {
                    Log.e(TAG, "Question document does not exist.");
                }
            } else {
                Log.e(TAG, "Error fetching question: " + task.getException().getMessage());
            }
        });
    }

    /**
     * Called when the user taps the send button.
     */
    public void onSend(View view) {
        EditText newMess = findViewById(R.id.newMess);
        String newMessage = newMess.getText().toString();
        if (!newMessage.isEmpty()) {
            helperMethods.sendMsg(newMessage);
            addMessageToView(newMessage, true);
            Log.d(TAG, "Sent message: " + newMessage);
        }
        newMess.setText("");
    }

    /**
     * Adds a message to the chat layout.
     * If isSentByChild is true, align right with the child's background;
     * otherwise, align left with the parent's background.
     */
    public void addMessageToView(String message, boolean isSentByChild) {
        runOnUiThread(() -> {
            LinearLayout chatLayout = findViewById(R.id.chat);
            if (chatLayout == null) {
                Log.e(TAG, "Chat layout not found in activity_main_par.xml.");
                return;
            }
            if (message.startsWith("ques?")) {
                String questionId = message.substring(5).trim();
                handleQuestionMessage(questionId);
                return;
            }
            TextView messageView = new TextView(this);
            messageView.setText(message);
            messageView.setTextSize(16);
            messageView.setPadding(10, 10, 10, 10);
            if (isSentByChild) {
                messageView.setBackgroundResource(R.drawable.sent_message_background);
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                messageView.setBackgroundResource(R.drawable.received_message_background);
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int marginBottom = (int) (8 * getResources().getDisplayMetrics().density);
            layoutParams.setMargins(0, 0, 0, marginBottom);
            messageView.setLayoutParams(layoutParams);
            chatLayout.addView(messageView);
            Log.d(TAG, "Message added: " + message);
        });
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
}
