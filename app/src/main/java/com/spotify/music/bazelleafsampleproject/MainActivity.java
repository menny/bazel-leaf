package com.spotify.music.bazelleafsampleproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.spotify.music.lib1.Lib1;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.hello_world);
        textView.setText(new Lib1().hello("Menny + Sean"));
    }
}
