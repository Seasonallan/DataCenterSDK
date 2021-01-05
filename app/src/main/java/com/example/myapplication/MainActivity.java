package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.library.net.Configure;
import com.library.net.EasyRequestApi;
import com.library.net.Event;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configure.context = getApplicationContext();

        EasyRequestApi.report(Event.START(true));


    }
}