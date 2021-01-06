package com.example.myapplication.ui;

import androidx.appcompat.app.AppCompatActivity;

import com.library.net.IPageCode;

public class BaseActivity extends AppCompatActivity implements IPageCode {

    @Override
    public String getPageCode() {
        return null;
    }
}