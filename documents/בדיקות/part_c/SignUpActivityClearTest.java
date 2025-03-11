package com.example.lenovo.eazyproject;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static org.hamcrest.Matchers.not;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.action.ViewActions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignUpActivityClearTest {

    @Rule
    public ActivityScenarioRule<SignUpActivity> activityRule =
            new ActivityScenarioRule<>(SignUpActivity.class);

    @Test
    public void testClearButtonClearsAllFields() {
        // Type text into each EditText field
        onView(withId(R.id.inputMailLogin))
                .perform(typeText("testUsername"), closeSoftKeyboard());
        onView(withId(R.id.inputMail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());
        onView(withId(R.id.inputPass))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.inputPass2))
                .perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.inputChildMail))
                .perform(typeText("child@example.com"), closeSoftKeyboard());

        // Select one of the radio buttons (e.g., Child)
        onView(withId(R.id.child)).perform(click());
        // (Optional) Verify the radio button is checked before clearing
        onView(withId(R.id.child)).check(matches(isChecked()));

        // Click the "Clear" button
        onView(withId(R.id.btnClear)).perform(click());

        // Verify that all EditText fields are now empty
        onView(withId(R.id.inputMailLogin)).check(matches(withText("")));
        onView(withId(R.id.inputMail)).check(matches(withText("")));
        onView(withId(R.id.inputPass)).check(matches(withText("")));
        onView(withId(R.id.inputPass2)).check(matches(withText("")));
        onView(withId(R.id.inputChildMail)).check(matches(withText("")));

        // Verify that no radio button is selected (both should not be checked)
        onView(withId(R.id.child)).check(matches(not(isChecked())));
        onView(withId(R.id.parent)).check(matches(not(isChecked())));
    }
}
