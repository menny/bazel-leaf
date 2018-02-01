package com.spotify.music.lib1;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Lib1Test {

    @Test
    public void testHello() {
        final Lib1 underTest = new Lib1();
        Assert.assertEquals("Hello Awesome! menny!", underTest.hello("menny"));
    }
}