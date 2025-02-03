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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    public String roomName = null;
    private static final String TAG = "MainActivity";
    boolean isActivityOn = true;
    AlertDialog logoutD;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomName = "testRoom"; // Hard-coded for debugging
        Log.d("MainActivity", "Hardcoded room name: " + roomName);

        helperMethods.init(this, roomName);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("messages")
                .whereEqualTo("room", roomName)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("MainActivity", "Error listening to messages", e);
                        return;
                    }

                    assert snapshots != null;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String message = dc.getDocument().getString("text");
                            Log.d("MainActivity", "Received message: " + message);
                            addMessageToView(message, false);
                        }
                    }
                });
    }

    /**
     * This method handles received Firebase messages and processes them appropriately.
     */
    public void onMessage(String message) {
        if (message == null || message.isEmpty()) {
            Log.e(TAG, "Received an empty or null message.");
            return;
        }

        // Check if the message is a question
        if (message.startsWith("ques:")) {
            // Extract question ID and handle the question
            String questionId = message.substring(5).trim();
            handleQuestionMessage(questionId);
        } else {
            // Display the message as a regular chat message
            runOnUiThread(() -> {
                LinearLayout chatLayout = findViewById(R.id.chat); // Chat layout
                if (chatLayout == null) {
                    Log.e(TAG, "Chat layout not found in activity_main.xml.");
                    return;
                }

                TextView messageView = new TextView(getApplicationContext());
                messageView.setText(message);
                chatLayout.addView(messageView); // Display the message in the chat layout
            });
        }
    }

    /**
     * This method handles Firebase-based responses for parent-sent questions and shows a dialog with answers.
     */
    public void handleQuestionMessage(String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            Log.e(TAG, "Question ID is null or empty.");
            return;
        }

        DocumentReference questionRef = db.collection("ques").document(questionId);

        questionRef.addSnapshotListener(this, (value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error fetching question: " + error.getMessage());
                return;
            }

            if (value != null && value.exists()) {
                runOnUiThread(() -> {
                    final Dialog dialog = new Dialog(MainActivity.this, R.style.Dialog);
                    dialog.setContentView(R.layout.custom); // Load dialog layout

                    String questionText = value.getString("text");
                    if (questionText == null) {
                        Log.e(TAG, "Question text is null in Firestore document.");
                        return;
                    }
                    dialog.setTitle(questionText + "?"); // Display the question text

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
        });
    }

    /**
     * This method handles the sending of a message entered by the user.
     */
    public void onSend(View view) {
        EditText newMess = findViewById(R.id.newMess);
        String newMessage = newMess.getText().toString();

        if (!newMessage.isEmpty()) {
            helperMethods.sendMsg(newMessage); // Send message via Firebase
            addMessageToView(newMessage, true); // Add message to chat
            Log.d("MainActivity", "Sent message: " + newMessage);
        }
        newMess.setText("");
    }



    /**
     * Adds a message to the chat layout.
     */
    public void addMessageToView(String message, boolean isSentByUser) {
        runOnUiThread(() -> {
            LinearLayout chatLayout = findViewById(R.id.chat);
            if (chatLayout == null) {
                Log.e("MainActivity", "Chat layout not found in activity_main.xml.");
                return;
            }

            // Create a basic TextView and add it to the layout
            TextView messageView = new TextView(this);
            messageView.setText(message);

            // Add to layout
            chatLayout.addView(messageView);
            Log.d("MainActivity", "Message added: " + message);
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
