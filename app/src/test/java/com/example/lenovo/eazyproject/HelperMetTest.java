package com.example.lenovo.eazyproject;


import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class HelperMetTest {

    @Test
    public void getImgId_Test_ReturnsCorrectId() {
        assertEquals(R.drawable.home, helperMethods.getImgId("ques1", "Home"));
        assertEquals(R.drawable.send, helperMethods.getImgId("ques1", "School"));
        assertEquals(R.drawable.their_message, helperMethods.getImgId("ques1", "Friend"));
    }
//        assertTrue(SignUpActivity.isEmailValid("name@gmail.com"));
//        assertTrue(SignUpActivity.isEmailValid("valid_email123@example.co.il"));
//        assertTrue(SignUpActivity.isEmailValid("eli@gmail.com"));
//        assertTrue(SignUpActivity.isEmailValid("
    }

