package com.example.lenovo.eazyproject;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SignUpActivityTest {

    @Test
    public void emailValidator_CorrectEmail_ReturnsTrue() {
        assertTrue(SignUpActivity.isEmailValid("name@gmail.com"));
        assertTrue(SignUpActivity.isEmailValid("valid_email123@example.co.il"));
        assertTrue(SignUpActivity.isEmailValid("eli@gmail.com"));
        assertTrue(SignUpActivity.isEmailValid("eylon@gmail.com"));
        assertTrue(SignUpActivity.isEmailValid("malice@gmail.com"));
    }

    @Test
    public void emailValidator_invalidEmail_ReturnsFalse() {
        assertFalse(SignUpActivity.isEmailValid("plainaddress"));
        assertFalse(SignUpActivity.isEmailValid("missing@domain"));
        assertFalse(SignUpActivity.isEmailValid("user@.com"));
        assertFalse(SignUpActivity.isEmailValid("user@com"));
        assertFalse(SignUpActivity.isEmailValid("user@domain..com"));
        assertFalse(SignUpActivity.isEmailValid("user@domain,com"));
        assertFalse(SignUpActivity.isEmailValid("@missingusername.com"));
        assertFalse(SignUpActivity.isEmailValid("user@domain.com (comment)"));
        assertFalse(SignUpActivity.isEmailValid("user@domain..com"));
    }
}