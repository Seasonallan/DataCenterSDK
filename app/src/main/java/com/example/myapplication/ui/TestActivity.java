package com.example.myapplication.ui;

import android.os.Bundle;

import com.example.myapplication.R;

public class TestActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        setTitle("页面加载日志");
    }


    @Override
    public String getPageCode() {
        return "TESTPAGE";
    }
}