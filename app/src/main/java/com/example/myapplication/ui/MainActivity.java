package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.myapplication.R;
import com.library.net.DataCenterEngine;
import com.library.net.DataEvent;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("功能选择");

        findViewById(R.id.list_btn_page).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TestActivity.class));
            }
        });
        findViewById(R.id.list_btn_event1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataCenterEngine.report(DataEvent.OPERATION("changeLanguage",
                        "切换语言", "之前【英语】修改成【中文】"));
            }
        });
        findViewById(R.id.list_btn_event2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataCenterEngine.report(DataEvent.OPERATION("changePwd", "修改密码", null));
            }
        });
        findViewById(R.id.list_btn_exception).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataEvent dataEvent = null;
                dataEvent.getEventType();
            }
        });

    }
}