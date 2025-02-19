package com.example.lenovo.eazyproject;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.lenovo.eazyproject.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class FirebaseLoginTest {

    private FirebaseAuth mockAuth;
    private FirebaseUser mockUser;
    private LoginActivity activity;

    @Before
    public void setUp() {
        mockAuth = mock(FirebaseAuth.class);
        mockUser = mock(FirebaseUser.class);
        activity = new LoginActivity();

        activity.inputMail = mock(EditText.class);
        activity.inputPass = mock(EditText.class);
        activity.btnLogin = mock(Button.class);
        activity.helloTxt = mock(TextView.class);
    }

    @Test
    public void testFirebaseLoginFailure() {
        when(mockAuth.getCurrentUser()).thenReturn(null);

        when(activity.inputMail.getText().toString()).thenReturn("test@example.com");
        when(activity.inputPass.getText().toString()).thenReturn("wrongpassword");

        activity.onClick(activity.btnLogin);

        verify(activity.helloTxt, never()).setText(contains("Hello,"));
    }
}
