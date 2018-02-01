package com.spotify.music.lib2;

import static com.google.common.base.Preconditions.checkNotNull;

public class Lib2 {
    public String awesome(String who) {
        return "Awesome! " + checkNotNull(who);
    }
}
