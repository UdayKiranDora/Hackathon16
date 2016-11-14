package com.eyeverify.eyeprintid;

import android.content.Intent;

import com.eyeverify.eyeprintid.GroupIdActivity;
import com.eyeverify.eyeprintid.SharedGlobals;
import com.eyeverify.eyeprintid.SplashActivity;

public class DemoSplashActivity extends SplashActivity {

    @Override
    protected Intent getLaunchIntent() {
        Intent launchIntent = new Intent(DemoSplashActivity.this, GroupIdActivity.class);
        return launchIntent;
    }
}
