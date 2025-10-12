package com.example.myapplication;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class ImageDatabase extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Map config = new HashMap();
        config.put("cloud_name", "diaw4uoea");
        config.put("api_key", "899786479714939");
        config.put("api_secret", "Opa-iYWYzcS3Z3mh1aqpfQeZncg");

        MediaManager.init(this, config);
    }
}
