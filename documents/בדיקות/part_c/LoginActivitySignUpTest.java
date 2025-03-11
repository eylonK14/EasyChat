package com.example.lenovo.eazyproject;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginActivitySignUpTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() {
        // Initialize Espresso Intents capturing
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Espresso Intents capturing
        Intents.release();
    }

    @Test
    public void testSignUpButtonOpensSignUpActivity() {
        // Click on the Signup button
        onView(withId(R.id.btnSignUp)).perform(click());

        // Verify that an intent was fired to start SignUpActivity
        intended(hasComponent(SignUpActivity.class.getName()));
    }
}
