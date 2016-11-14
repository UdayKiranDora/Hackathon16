package com.eyeverify.eyeprintid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eyeverify.evserviceinterface.client.EVEnrollCompletion;
import com.eyeverify.evserviceinterface.client.EVRegisterCompletion;
import com.eyeverify.evserviceinterface.client.EVServiceClient;
import com.eyeverify.evserviceinterface.client.EVServiceListener;
import com.eyeverify.evserviceinterface.client.EVVerifyCompletion;
import com.eyeverify.evserviceinterface.constants.EVEnums;
import com.eyeverify.evserviceinterface.constants.EVError;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class GroupIdActivity extends Activity {

    public static final String TAG = GroupIdActivity.class.getSimpleName();

    private static final String LEARN_URL = "http://www.eyeverify.com/learn-more";
    private static final String NEW_URL = "http://info.eyeverify.com/groupID";

    private AutoCompleteTextView group_id_entry_field;

    private EVEnums.abort_reason licenseErrorType;

    private EVServiceClient mServiceClient;

    /**
     * Check Compliance Async Task
     */
    private class CheckComplianceTask extends AsyncTask<String, Void, Set<EVError>> {

        @Override
        protected Set<EVError> doInBackground(String... params) {
            HashSet<EVError> errors = new HashSet<EVError>();
            try {
                mServiceClient.setLicenseCertificate(BaseActivity.readLicenseCertificate());
                mServiceClient.checkCompliance("Android Validate License", errors);
            } catch (Throwable ex) {
                String msg = "Failed to check license.";
                Log.e(TAG, msg, ex);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }

            return errors;
        }

        @Override
        protected void onPostExecute(Set<EVError> errors) {
            for (EVError error : errors) {
                if (error==EVError.EVInvalidLicenseError) {
                    Log.w(TAG, "License is invalid.");
                    BaseActivity.saveLicenseCertificate(null);
                    showAlertDialog("Invalid license", error.getDescription());
                    return;
                }
            }

            for (EVError error : errors) {
                if (error==EVError.EVUnsupportedDeviceError) {
                    Log.w(TAG, "Unsupported Device.");
                    BaseActivity.saveLicenseCertificate(null); // show Group ID screen on next app start
                    showAlertDialog("Unsupported Device", error.getDescription());
                    return;
                }
            }

            Intent intent = new Intent(GroupIdActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);

            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (BaseActivity.readLicenseCertificate()!=null) {
            Intent intent = new Intent(GroupIdActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);

            finish();
            return;
        }

        setContentView(R.layout.activity_group_id);
        getExtras();
        setTitleText();
        ifNeededDisplayEula();
        setupEulaAcceptButton();
        setupSubmitLicenseButton();
        setupRequestGroupIdButton();
        setupLearnMoreButton();
        setupGroupIdTextField();

        mServiceClient = new EVServiceClient(mListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mServiceClient!=null) {
            try {
                mServiceClient.connect(getResources().getString(R.string.servicePackageId), getBaseContext(), null);
            } catch (Throwable ex) {
                String msg = "Failed to connect.";
                Log.e(TAG, msg, ex);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        if (mServiceClient!=null) {
            try {
                mServiceClient.disconnect();
            } catch (Throwable ex) {
                String msg = "Failed to disconnect.";
                Log.e(TAG, msg, ex);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        }

        super.onPause();
    }

    private void setupGroupIdTextField() {
        group_id_entry_field = (AutoCompleteTextView) findViewById(R.id.group_id_field);
        group_id_entry_field.setText("");
        group_id_entry_field.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getX() > group_id_entry_field.getWidth() * 2 / 3) {
                    group_id_entry_field.clearFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(group_id_entry_field.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
    }

    private void setTitleText() {
        if (licenseErrorType != null) {
            TextView groupIdFieldTitleText = (TextView)findViewById(R.id.group_id_field_title_text);
            switch (licenseErrorType)
            {
                case license_invalid:
                    groupIdFieldTitleText.setText(R.string.capture_license_invalid_suggestion);
                    break;
                case license_expired:
                    groupIdFieldTitleText.setText(R.string.capture_license_expired_suggestion);
                    break;
                case license_limited:
                    groupIdFieldTitleText.setText(R.string.capture_license_expired_suggestion);
                    break;
                default:
                    groupIdFieldTitleText.setText(R.string.title_activity_group_id);
                    break;
            }
        }
    }

    private void getExtras() {
        licenseErrorType = (EVEnums.abort_reason)getIntent().getSerializableExtra(SharedGlobals.LICENSE_ERROR);
    }

    private void setupLearnMoreButton() {
        Button learnMoreButton = (Button)findViewById(R.id.learn_more_button);
        learnMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(LEARN_URL));
                startActivity(browserIntent);
            }
        });
    }

    private void setupRequestGroupIdButton() {
        Button requestGroupId = (Button)findViewById(R.id.request_group_id_button);
        requestGroupId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(NEW_URL));
                startActivity(browserIntent);
            }
        });
    }

    private void setupSubmitLicenseButton() {
        Button submitButton = (Button)findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AutoCompleteTextView groupIdField = (AutoCompleteTextView)findViewById(R.id.group_id_field);
                String license_certificate = groupIdField.getText().toString();
                if ( license_certificate.trim().length() == 0) {
                    showAlertDialog(getString(R.string.error_empty_license_title), getString(R.string.error_empty_license_message));
                    return;
                }

                BaseActivity.saveLicenseCertificate(license_certificate);
                new CheckComplianceTask().execute();
            }
        });
    }

    private void setupEulaAcceptButton() {
        final ViewGroup eulaLayout = (ViewGroup)findViewById(R.id.eula_layout);
        final ViewGroup groupIdLayout = (ViewGroup)findViewById(R.id.group_id_layout);
        Button accept_button = (Button) findViewById(R.id.eula_button);
        accept_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                eulaLayout.setVisibility(View.GONE);
                groupIdLayout.setVisibility(View.VISIBLE);
                SharedPreferences prefs = getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("eula", Boolean.TRUE);
                editor.apply();
                editor.commit();
            }
        });
    }

    private void ifNeededDisplayEula() {
        final ViewGroup eulaLayout = (ViewGroup)findViewById(R.id.eula_layout);
        final ViewGroup groupIdLayout = (ViewGroup)findViewById(R.id.group_id_layout);
        SharedPreferences prefs = getSharedPreferences(SharedGlobals.SHARED_PREFERENCES, Activity.MODE_PRIVATE);
        if (!prefs.getBoolean("eula", Boolean.FALSE)) {
            String installationSource = getPackageManager().getInstallerPackageName(getPackageName());
            if (installationSource==null || !installationSource.startsWith("com.google")) {
                eulaLayout.setVisibility(View.VISIBLE);
                groupIdLayout.setVisibility(View.GONE);

            }
        }
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // ok
            }
        });
        alertDialog.show();
    }

    /**
     * EV Service Listener
     */
    EVServiceListener mListener = new EVServiceListener() {

        @Override
        public void onServiceAvailable() {
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
        public void registrationCompleted(EVRegisterCompletion completion) {
        }

        @Override
        public void verificationCompleted(EVVerifyCompletion completion) {
        }
    };

}
