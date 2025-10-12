package com.example.myapplication;

import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration override = new Configuration(newBase.getResources().getConfiguration());
        override.fontScale = 1.0f;
        Context context = newBase.createConfigurationContext(override);
        super.attachBaseContext(context);
    }
}
