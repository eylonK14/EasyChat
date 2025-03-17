package com.example.lenovo.eazyproject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnSignOut;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;  // Firestore field
    public String roomName = null;
    private static final String TAG = "MainActivity";
    boolean isActivityOn = true;
    AlertDialog logoutD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        btnSignOut = findViewById(R.id.btnSignOut);

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut(); // Logs out the user
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Retrieve room name from extras
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            roomName = extras.getString("roomName");
        } else {
            Log.e(TAG, "Room name is null. Cannot proceed.");
            return;
        }

        // Initialize helper methods and Firestore instance
        helperMethods.init(this, roomName);
        db = FirebaseFirestore.getInstance();

        // Listen for messages in the room and display full history.
        // Now, we check the "sender" field: if "child" then it's the child's message (align right),
        // otherwise, assume it's from the parent (align left).
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
                            LinearLayout chatLayout = findViewById(R.id.chat);
                            if (chatLayout == null) {
                                Log.e(TAG, "Chat layout not found in activity_main.xml.");
                                return;
                            }
                            // Clear the layout before reloading messages
                            chatLayout.removeAllViews();
                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                String message = document.getString("text");
                                String sender = document.getString("sender");
                                // For MainActivity (child device), if sender equals "child", then the message was sent by the child.
                                // Otherwise, it was sent by the parent.
                                boolean isSentByChild = (sender != null && sender.equals("child"));
                                if (message != null) {
                                    Log.d(TAG, "Adding message: " + message + " (sender: " + sender + ")");
                                    addMessageToView(message, isSentByChild);
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

    /**
     * Handles received Firebase messages (for questions and chat messages).
     */
    public void onMessage(String message) {
        if (message == null || message.isEmpty()) {
            Log.e(TAG, "Received an empty or null message.");
            return;
        }
        // If the message is a question, handle it.
        if (message.startsWith("ques")) {
            String questionId = message.substring(4).trim();
            handleQuestionMessage(questionId);
        } else {
            // Otherwise, simply add it to the chat view.
            runOnUiThread(() -> {
                LinearLayout chatLayout = findViewById(R.id.chat);
                if (chatLayout == null) {
                    Log.e(TAG, "Chat layout not found in activity_main.xml.");
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
        // Use get() instead of addSnapshotListener
        questionRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot value = task.getResult();
                if (value != null && value.exists()) {
                    runOnUiThread(() -> {
                        final Dialog dialog = new Dialog(MainActivity.this, R.style.Dialog);
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
            // Use helperMethods.sendMsg so that the message gets a "sender" field.
            helperMethods.sendMsg(newMessage);
            // Locally add the message; mark as sent by the child.
            addMessageToView(newMessage, true);
            Log.d(TAG, "Sent message: " + newMessage);
        }
        newMess.setText("");
    }

    /**
     * Adds a message to the chat layout.
     * If isSentByChild is true, it means the message was sent by the child (this user),
     * so it will be aligned to the right with a "child" background.
     * Otherwise, it is from the parent and will be aligned to the left.
     */
    public void addMessageToView(String message, boolean isSentByChild) {
        runOnUiThread(() -> {
            LinearLayout chatLayout = findViewById(R.id.chat);
            if (chatLayout == null) {
                Log.e(TAG, "Chat layout not found in activity_main.xml.");
                return;
            }

            if (message.startsWith("ques")) {
                String questionId = message.substring(4).trim();
                handleQuestionMessage(questionId);
            }

            TextView messageView = new TextView(this);
            messageView.setText(message);
            messageView.setTextSize(16);
            messageView.setPadding(10, 10, 10, 10);

            if (isSentByChild) {
                // Child's message: right-aligned, white (or received) background.
                messageView.setBackgroundResource(R.drawable.sent_message_background);
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                // Parent's message: left-aligned, green (or sent) background.
                messageView.setBackgroundResource(R.drawable.received_message_background);
                messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }

            // Set layout parameters with a bottom margin.
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
