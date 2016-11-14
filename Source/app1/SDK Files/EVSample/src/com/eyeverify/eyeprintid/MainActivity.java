package com.eyeverify.eyeprintid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eyeverify.evserviceinterface.client.EVEnrollCompletion;
import com.eyeverify.evserviceinterface.client.EVRegisterCompletion;
import com.eyeverify.evserviceinterface.client.EVServiceClient;
import com.eyeverify.evserviceinterface.client.EVServiceException;
import com.eyeverify.evserviceinterface.client.EVServiceListener;
import com.eyeverify.evserviceinterface.client.EVVerifyCompletion;
import com.eyeverify.evserviceinterface.client.base.EVServiceProperties;
import com.eyeverify.evserviceinterface.constants.EVPermissionInfo;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class  MainActivity extends BaseActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String USERNAME_PREFERENCE = "username";

    private AutoCompleteTextView user_entry_username_field;
    private Button user_entry_enroll_button;
    private Button user_entry_verify_button;
    private Button user_entry_setting_button;
    private TextView user_entry_version_text;
    private Button user_entry_language_button;
    private Locale mLocale;

    private boolean mNotifiedUser;
    private EVServiceClient mServiceClient;
    private boolean isPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mServiceClient = new EVServiceClient(mListener, new EVServiceProperties(readLicenseCertificate()));

        user_entry_version_text = (TextView) findViewById(R.id.user_entry_version_text);

        user_entry_username_field = (AutoCompleteTextView) findViewById(R.id.user_entry_username_field);
        user_entry_username_field.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getX() > user_entry_username_field.getWidth() * 2 / 3) {
                    //volume on button
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        setIsAudioEnabled(!isAudioEnabled());
                    }
                    user_entry_username_field.clearFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(user_entry_username_field.getWindowToken(), 0);

                    return true;
                }

                return false;
            }
        });
        user_entry_username_field.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                enableDisableButtons();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        user_entry_enroll_button = (Button) findViewById(R.id.user_entry_enroll_button);
        user_entry_enroll_button.setEnabled(false);
        user_entry_enroll_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

				if (!validateUserName()) {
                    return;
                }

                boolean isEnrolled = false;
                try {
                    isEnrolled = mServiceClient.isUserEnrolled(getUserName());
                    System.out.println("pause");
                } catch (Throwable ex) {
                    Log.w(TAG, "Failed to check user enrolled status", ex);
                }

                if (isEnrolled) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Enrollments")
                            .setMessage("Proceeding will delete your previous enrollments.")
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                    pushEnrollment(false);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                    dialog.cancel();
                                }
                            })
                            .show();


                } else {
                    pushEnrollment(true);
                }
            }
        });

        user_entry_verify_button = (Button) findViewById(R.id.user_entry_verify_button);
        user_entry_verify_button.setEnabled(false);
        user_entry_verify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateUserName()) {
                    return;
                }

                Intent intent = new Intent(getBaseContext(), EVCaptureActivity.class);
                intent.putExtra(EVCaptureActivity.IS_ENROLLMENT_KEY, false);
                intent.putExtra(EVCaptureActivity.USER_ID_KEY, getUserName());
                intent.putExtra(EVCaptureActivity.SERVICE_PACKAGE_KEY, getResources().getString(R.string.servicePackageId));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                pushActivity(intent);
            }
        });

        user_entry_setting_button = (Button) findViewById(R.id.user_entry_setting_button);
        user_entry_setting_button.setEnabled(false);
        if (getResources().getBoolean(R.bool.showSettings)) {
            user_entry_setting_button.setVisibility(View.VISIBLE);
            user_entry_setting_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Log.d(TAG, "Executing isUserEnrolled #1...");
                        boolean isEnrolled = mServiceClient.isUserEnrolled(getUserName());
                        Log.d(TAG, "isEnrolled="+isEnrolled);
                    } catch (Throwable ex) {
                        Log.w(TAG, "Failed to check user enrolled status.", ex);
                    }

                    try {
                        Log.d(TAG, "Executing getEnrolledUsers...");
                        String[] enrolledUsers = mServiceClient.getEnrolledUsers();
                        Log.d(TAG, "enrolledUsers ="+enrolledUsers);
                    } catch (Throwable ex) {
                        Log.w(TAG, "Failed to get enrolled users.", ex);
                    }

                    try {
                        Log.d(TAG, "Executing getSettings...");
                        TreeMap<String, String> settings = (TreeMap<String, String>)mServiceClient.getSettings();
                        Log.d(TAG, "settings="+settings);

                        Intent intent = new Intent(getBaseContext(), SettingActivity.class);
                        intent.putExtra(SettingActivity.SETTINGS_EXTRA, settings);
                        pushActivity(intent);

                    } catch (Throwable ex) {
                        Log.w(TAG, "Failed to check user's EyeVerify enrollment status", ex);
                    }
                }
            });
        } else {
            user_entry_setting_button.setVisibility(View.GONE);
        }

        user_entry_language_button = (Button) findViewById(R.id.user_entry_language_button);
        user_entry_language_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLocale();
            }
        });

        audioEnabledChanged();


    }

    private void pushEnrollment(boolean showInstructions) {
        Intent intent;

        if (showInstructions) {
            intent = new Intent(getBaseContext(), EVCaptureActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        } else {
            intent = new Intent(getBaseContext(), EVCaptureActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        intent.putExtra(EVCaptureActivity.IS_ENROLLMENT_KEY, true);
        intent.putExtra(EVCaptureActivity.USER_ID_KEY, getUserName());
        intent.putExtra(SharedGlobals.NEXT_ACTIVITY, MainActivity.class);
        intent.putExtra(EVCaptureActivity.USERKEY_KEY, "TEST");
        intent.putExtra(EVCaptureActivity.SERVICE_PACKAGE_KEY, getResources().getString(R.string.servicePackageId));

        pushActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNotifiedUser = false;
        isPaused = false;

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        user_entry_username_field.setText(prefs.getString(USERNAME_PREFERENCE, null));

        try {
            mServiceClient.connect(getResources().getString(R.string.servicePackageId), getBaseContext(), null);
            checkCameraPermission();

        } catch (Throwable ex) {
            String msg = "Failed to connect.";
            Log.e(TAG, msg, ex);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    protected void onPause() {

        isPaused = true;

        try {
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            prefs.edit().putString(USERNAME_PREFERENCE, getUserName()).commit();

            if (!hasPushedActivity) mServiceClient.releaseCamera();
            mServiceClient.disconnect(!hasPushedActivity);
        } catch (Throwable ex) {
            String msg = "Failed to disconnect.";
            Log.e(TAG, msg, ex);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }

        super.onPause();
    }

    @Override
    protected void setIsAudioEnabled(boolean audioEnabled) {
        super.setIsAudioEnabled(audioEnabled);
        audioEnabledChanged();
    }

    private void audioEnabledChanged() {

        user_entry_username_field.setBackground(getResources().getDrawable(isAudioEnabled() ? com.eyeverify.eyeprintid.R.drawable.wide_button_username_v_on_bg : com.eyeverify.eyeprintid.R.drawable.wide_button_username_v_off_bg));
    }


    private void toggleLocale() {
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();

        Locale current = res.getConfiguration().locale;

        boolean isChina = current.equals(Locale.CHINESE);

        mLocale = isChina ? Locale.ENGLISH : Locale.CHINESE;

        conf.locale = mLocale;
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }

    private String getUserName() {
        return user_entry_username_field.getText().toString().toLowerCase().trim();
    }

    private boolean validateUserName() {
        String username = getUserName();

        if (username==null || username.trim().length()==0) {
            new AlertDialog.Builder(this)
                    .setTitle("Missing User Name")
                    .setMessage("Please enter a user name.")
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            dialog.cancel();
                        }
                    })
                    .show();
            return false;
        }
        else if (!Character.isAlphabetic(username.charAt(0)) || !username.matches("[A-Za-z0-9]+")) {
            new AlertDialog.Builder(this)
                    .setTitle("Invalid User Name")
                    .setMessage("User name must start with a letter and should contain only alphanumeric symbols without spaces.")
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            dialog.cancel();
                        }
                    })
                    .show();
            return false;
        }

        return true;
    }

    private void enableDisableButtons() {

        String userName = getUserName();

        boolean isEnrolled = false;
        boolean isBusy = true;
   //     boolean hasCamera = false;
        boolean isConnected = false;
        if (mServiceClient.isConnected()) {

            if (mPermissionState == kPERMISSION_STATE_GRANTED) {
                try {
                    if (userName != null && userName.trim().length() > 0) {
                        isEnrolled = mServiceClient.isUserEnrolled(userName);
                    }

                    isBusy = mServiceClient.isAuthenticatorBusy();
                    //         hasCamera = mServiceClient.getCamera();
                    isConnected = true;
                } catch (EVServiceException e) {
                }

                if (isBusy && !isPaused) {

                    Log.d(TAG, "Authenticator is " + (isConnected ? "busy" : "not connected") +  ", waiting...");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            enableDisableButtons();
                        }
                    }, 500);
                }

                enableDisableButtons(isEnrolled, isBusy);

            }
        }
    }

    private void enableDisableButtons(boolean enrolled, boolean authBusy) {

        user_entry_setting_button.setEnabled(true);

        boolean canAuth = getUserName().length() > 0 && !authBusy;

        if (enrolled) {
            user_entry_enroll_button.setText(com.eyeverify.eyeprintid.R.string.main_recreate_button);
            user_entry_enroll_button.setEnabled(canAuth);
            user_entry_verify_button.setEnabled(canAuth);
        } else {
            user_entry_enroll_button.setText(com.eyeverify.eyeprintid.R.string.main_create_button);
            user_entry_enroll_button.setEnabled(canAuth);
            user_entry_verify_button.setEnabled(false);
        }
    }

    public void checkCameraPermission()  {

        EVPermissionInfo[] arr = null;
        if (mServiceClient != null && mServiceClient.isConnected()) {
            try {
                arr = mServiceClient.getPermissionInfo();
            } catch (EVServiceException ex) {
                mPermissionState = BaseActivity.kPERMISSION_STATE_UNKNOWN;
                Log.d("yyz", "Permission is Unknown");
                return;
            }
        }

        if (arr != null) {
            for(int i = 0; i < arr.length; i++)
            {
                EVPermissionInfo info = arr[i];
                if(info != null) {
                    if (info.permission.compareTo(Manifest.permission.CAMERA) == 0) {
                        if (info.granted == EVPermissionInfo.Approval.GRANTED)
                            mPermissionState = BaseActivity.kPERMISSION_STATE_GRANTED;
                        else
                            mPermissionState = BaseActivity.kPERMISSION_STATE_REVOKED;
                    }
                }
            }
            Log.d("yyz", "Permission is " + mPermissionState);
        } else {
            mPermissionState = BaseActivity.kPERMISSION_STATE_REVOKED;
            Log.d("yyz", "Permission is Revoked");
        }
    }


    public boolean doUpdates = true;


    EVServiceListener mListener = new EVServiceListener() {

        @Override
        public void onServiceAvailable() {

            checkCameraPermission();
            if (mPermissionState == kPERMISSION_STATE_GRANTED) {
                try {
                    enableDisableButtons();
                    boolean hasCamera = mServiceClient.getCamera();
                    if (!hasCamera)
                        Toast.makeText(getApplicationContext(), "Camera is not available. Please ensure the application has permissions.", Toast.LENGTH_LONG).show();
                    user_entry_version_text.setText(mServiceClient.getVersion());
                } catch (EVServiceException ex) {
                    Log.w(TAG, "Failed to enable buttons.", ex);
                }
            } else if ( (mPermissionState == kPERMISSION_STATE_REVOKED) && doUpdates) {
                Toast.makeText(getApplicationContext(), "Camera Permissions have been REVOKED.", Toast.LENGTH_LONG).show();
                doUpdates = false;
            }

        }

        @Override
        public void onWindowAdded() {

        }

        @Override
        public void onWindowRemoved() {

        }

        @Override
        public void onWindowFailure() {

        }

        @Override
        public void handleEvent(int eventCode, Map<String, String> params) {

        }

        @Override
        public void enrollmentCompleted(EVEnrollCompletion completion) {

        }

        @Override
        public void registrationCompleted(EVRegisterCompletion completion)
        {

        }

        @Override
        public void verificationCompleted(EVVerifyCompletion completion) {

        }
    };

}
