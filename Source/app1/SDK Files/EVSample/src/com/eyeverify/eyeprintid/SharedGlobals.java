package com.eyeverify.eyeprintid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.eyeverify.evserviceinterface.constants.EVEnums;

public class SharedGlobals {

    protected static final String SHARED_PREFERENCES = "EyeVerify-SharedPreferences";
    private static final String LICENSE_PREFERENCE = "Elicense";

    public static final String NEXT_ACTIVITY = "NEXT_ACTIVITY";
    public static final String LICENSE_ERROR = "LICENSE_ERROR";

    public enum ERROR_TYPE { NOT_ENROLLED, NOT_VERIFIED, DISTANCE, NO_EYE, QUALITY,
        SYSTEM, LICENSE_INVALID, LICENSE_EXPIRED, LICENSE_LIMITED, INTERNET, ENROLLMENT_MATCH, CHAFF, APP_BACKGROUND, LOW_LIGHTING, NOT_SUPPORTED, CAMERA_EXCEPTION }

    public static boolean isLicensingError(EVEnums.abort_reason errorType) {
        return (errorType == EVEnums.abort_reason.license_expired) || (errorType == EVEnums.abort_reason.license_invalid) || (errorType == EVEnums.abort_reason.license_limited);
    }

    public static String readLicenseCertificate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        return prefs.getString(LICENSE_PREFERENCE, null);
    }

    public static void saveLicenseCertificate(Context context, String license_certificate) {
        SharedPreferences prefs = context.getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (license_certificate != null) {
            editor.putString(LICENSE_PREFERENCE, license_certificate);
        } else {
            editor.remove(LICENSE_PREFERENCE);
        }
        editor.apply();
        editor.commit();
    }

    public static void deleteLicenseCertificate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(LICENSE_PREFERENCE);
        editor.apply();
        editor.commit();
    }

}
