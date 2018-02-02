package com.spotify.music.lib2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class Lib2Test {

    @Test
    public void testAwesome() {
        final Lib2 underTest = new Lib2();
        Assert.assertEquals("Awesome! menny", underTest.awesome("menny"));
        //Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void testNPE() {
        final Lib2 underTest = new Lib2();
        underTest.awesome(null);
    }
}