package com.example.lenovo.eazyproject;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class helperMethods {

    @SuppressLint("StaticFieldLeak")
    private static final FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
    private static String roomName;

    public static void init(MainActivity mainActivity, String _roomName) {
        roomName = _roomName;
        subscribeToRoom();
    }

    public static void init(MainActivityPar mainActivityPar, String _roomName) {
        roomName = _roomName;
        subscribeToRoom();
    }

    private static void subscribeToRoom() {
        firebaseMessaging.subscribeToTopic(roomName)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        System.out.println("Failed to subscribe to room topic");
                    }
                });
    }

    public static void sendMsg(String msg) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", msg);
        messageData.put("room", roomName); // Ensure `roomName` is set
        messageData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("messages").add(messageData)
                .addOnSuccessListener(documentReference -> Log.d("HelperMethods", "Message sent: " + msg))
                .addOnFailureListener(e -> Log.e("HelperMethods", "Error sending message", e));
    }


    public static int getImgId (String quesId, String ansId)
    {
        if (quesId.equals("ques1")) // במידה והשאלה היא שאלה 1 - החזרת תשובה מתוך התשובות האפשריות, בהתאם לתשובה של הילד
        {
            if (ansId.equals("Home"))
                return(R.drawable.home);
            if (ansId.equals("School"))
                return(R.drawable.send);
            if (ansId.equals("Friend"))
                return(R.drawable.their_message);
        }

        if (quesId.equals("ques2")) // במידה והשאלה היא שאלה 2 - החזרת תשובה מתוך התשובות האפשריות, בהתאם לתשובה של הילד
        {
            if (ansId.equals("Yes"))
                return(R.drawable.home);
            if (ansId.equals("No"))
                return(R.drawable.send);
        }

        return -1; // אם לא הוחזר כלום במהלך הפעולה קרתה שגיאה ונחזיר מינוס 1
    }
}