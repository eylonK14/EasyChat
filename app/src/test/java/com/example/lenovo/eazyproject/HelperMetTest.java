package com.example.lenovo.eazyproject;

import static junit.framework.TestCase.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HelperMetTest {

    @Test
    public void getImgId_Test_ReturnsCorrectId() {
        assertEquals(R.drawable.home, helperMethods.getImgId("ques1", "Home"));
        assertEquals(R.drawable.send, helperMethods.getImgId("ques1", "School"));
        assertEquals(R.drawable.their_message, helperMethods.getImgId("ques1", "Friend"));
    }
}

