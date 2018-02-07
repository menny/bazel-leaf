package com.spotify.music.andlib;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MyParcelableTest {

    @Test
    public void testHappy() {
        Assert.assertNotNull(new MyParcelable());
    }

}