package com.eyeverify.eyeprintid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

public abstract class BaseActivity extends Activity {

    public static final int kPERMISSION_STATE_UNKNOWN = -1;
    public static final int kPERMISSION_STATE_REVOKED = 0;
    public static final int kPERMISSION_STATE_GRANTED = 1;

    protected int mPermissionState = kPERMISSION_STATE_UNKNOWN;

    protected boolean hasPushedActivity;
    static Context _context;

    @Override
    protected void onStart() {

        super.onStart();

//        * @param enterAnim A resource ID of the animation resource to use for
//        * the incoming activity.  Use 0 for no animation.
//        * @param exitAnim A resource ID of the animation resource to use for
//        * the outgoing activity.  Use 0 for no animation.

        if (hasPushedActivity) {
            this.overridePendingTransition(R.anim.left_slide_out, R.anim.left_slide_in);
        }
        hasPushedActivity = false;
    }

    protected boolean isAudioEnabled() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        boolean result = prefs.getBoolean("audio_enabled", Boolean.FALSE);
        return result;
    }

    protected void setIsAudioEnabled(boolean audio_enabled) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("audio_enabled", audio_enabled).apply();
    }

    protected void pushActivity(Intent intent) {

        hasPushedActivity = true;
        startActivityForResult(intent, 0);

        overridePendingTransition(R.anim.right_slide_in, R.anim.right_slide_out);
    }
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        _context = getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getApplicationContext().getResources().getConfiguration().smallestScreenWidthDp < 800) {

//          New code
            if (Build.MANUFACTURER.equals("SHARP") && Build.MODEL.equals("SH-02H")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

//            existing code
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);

        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public static Context getContext() {
        return _context;
    }


    public static String readLicenseCertificate() {
        return SharedGlobals.readLicenseCertificate(getContext());
    }

    public static void saveLicenseCertificate(String license_certificate) {
        SharedGlobals.saveLicenseCertificate(getContext(), license_certificate);
    }

    public static void deleteLicenseCertificate() {
        SharedGlobals.deleteLicenseCertificate(getContext());
    }

    /*
     *  instance variables are not visible from inner classes, but methods are.
     *  I added this getter and setter with the express purpose of updating
     *  this variable from within EVServiceClient --> onServiceAvailable()
     */
//    public int getPermissionState() {
//        return mPermissionState;
//    }
//
//    public void setPermissionState(int state) {
//        this.mPermissionState = state;
//        Toast.makeText(this, "Camera Permission granted: " + state, Toast.LENGTH_LONG);
//    }
}
