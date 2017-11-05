package com.spotify.music.lib1;

import com.spotify.music.lib2.Lib2;
import com.spotify.music.lib2.Lib3;

public class Lib1 {
    public String hello(String who) {
        return Lib3.TEST + "Hello " + new Lib2().awesome(who) + "!";
    }
}
