package com.eyeverify.eyeprintid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public abstract class SplashActivity extends BaseActivity {


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                startActivity(getLaunchIntent());
                finish();
            }
        }, 2000);

    }

    protected abstract Intent getLaunchIntent();
}
