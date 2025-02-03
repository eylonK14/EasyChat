package com.example.lenovo.eazyproject;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

public class myListener {

    private MainActivity mainActivity;
    private MainActivityPar mainActivityPar;
    private String roomName;

    public myListener(MainActivity mainActivity, String roomName) {
        this.mainActivity = mainActivity;
        this.roomName = roomName;
        listenToMessages();
    }

    public myListener(MainActivityPar mainActivityPar, String roomName) {
        this.mainActivityPar = mainActivityPar;
        this.roomName = roomName;
        listenToMessages();
    }

    private void listenToMessages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("messages").whereEqualTo("room", roomName)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        System.out.println("Listen failed: " + e.getMessage());
                        return;
                    }
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String msg = dc.getDocument().getString("text");
                            if (mainActivity != null) {
                                mainActivity.onMessage(msg);
                            } else if (mainActivityPar != null) {
                                mainActivityPar.onMessage(msg);
                            }
                        }
                    }
                });
    }
}
