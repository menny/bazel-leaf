package com.spotify.music.bazelleafsampleproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.spotify.music.andlib.MyParcelable;
import com.spotify.music.lib1.Lib1;
import com.spotify.music.lib3.Lib3;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.lib1_text)).setText(new Lib1().hello("Menny + Sean"));
        ((TextView) findViewById(R.id.lib3_text)).setText(Lib3.LIB3_TEST);
        ((TextView) findViewById(R.id.andlib_text)).setText(R.string.andlib1);
        MyParcelable parcelable = new MyParcelable();
    }
}
