package com.spotify.gradle.bazel.utils;

import org.gradle.wrapper.Logger;

/**
 * An implementation of {@link Logger} which also flush the output
 * immediately after writing to it.
 */
public class LoggerWithFlush extends Logger {

    public LoggerWithFlush() {
        super(false);
    }

    @Override
    public void log(String message) {
        super.log(message);
        System.out.flush();
    }

    @Override
    public Appendable append(char c) {
        super.append(c);
        System.out.flush();
        return this;
    }

    @Override
    public Appendable append(CharSequence csq) {
        super.append(csq);
        System.out.flush();
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) {
        super.append(csq, start, end);
        System.out.flush();
        return this;
    }
}
