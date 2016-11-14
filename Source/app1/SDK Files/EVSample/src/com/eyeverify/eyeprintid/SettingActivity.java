package com.eyeverify.eyeprintid;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.eyeverify.evserviceinterface.client.EVEnrollCompletion;
import com.eyeverify.evserviceinterface.client.EVRegisterCompletion;
import com.eyeverify.evserviceinterface.client.EVServiceClient;
import com.eyeverify.evserviceinterface.client.EVServiceListener;
import com.eyeverify.evserviceinterface.client.EVVerifyCompletion;
import com.eyeverify.evserviceinterface.client.base.EVServiceProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SettingActivity extends PreferenceActivity {
    public static final String TAG = SettingActivity.class.getSimpleName();

    public static final String SETTINGS_EXTRA = "SETTINGS";

    private EVServiceClient mServiceClient;

    class PreferenceChangeListener implements Preference.OnPreferenceChangeListener {
        private EditTextPreference preference;

        PreferenceChangeListener(EditTextPreference preference) {
            this.preference = preference;
        }

        public boolean onPreferenceChange(Preference preference, Object value) {
            String name = preference.getTitle().toString();
            try {
                preference.setSummary(String.valueOf(value));
                mServiceClient.setSetting(name, value.toString());
            } catch (Throwable ex) {
                Log.w(TAG, "Failed to change EyeVerify setting: name="+preference.getTitle()+"; value="+value, ex);
            }
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent k= new Intent(this,LanActivity.class);
        startActivity(k);
        /*Intent in=getIntent();
        Bundle bd=in.getExtras();
        if (bd.get("value")=="ok")
        {
            Intent k=new Intent(this,LanActivity.class);
            startActivity(k);
        }*/
        String licenseCertificate = SharedGlobals.readLicenseCertificate(getApplicationContext());
        mServiceClient = new EVServiceClient(mListener, new EVServiceProperties(licenseCertificate));

        try {
            TreeMap<String, String> settings = new TreeMap<String, String>((HashMap<String, String>)getIntent().getSerializableExtra(SETTINGS_EXTRA));

            PreferenceScreen root = getPreferenceManager().createPreferenceScreen(SettingActivity.this);
            for (Map.Entry<String, String> setting : settings.entrySet()) {
                EditTextPreference preference = new EditTextPreference(SettingActivity.this);
                preference.setTitle(setting.getKey());
                preference.setSummary(setting.getValue());
                preference.setText(setting.getValue());
                preference.setOnPreferenceChangeListener(new PreferenceChangeListener(preference));
                root.addPreference(preference);
            }
            setPreferenceScreen(root);
        } catch (Throwable ex) {
            Log.w(TAG, "Failed to read EyeVerify settings.", ex);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            mServiceClient.connect(getResources().getString(R.string.servicePackageId), getBaseContext(), null);
        } catch (Throwable ex) {
            String msg = "Failed to connect.";
            Log.e(TAG, msg, ex);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    protected void onPause() {
        try {
            mServiceClient.disconnect();
        } catch (Throwable ex) {
            String msg = "Failed to disconnect.";
            Log.e(TAG, msg, ex);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }

        super.onPause();
    }

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
        public void registrationCompleted(EVRegisterCompletion completion)
        {
        }

        @Override
        public void verificationCompleted(EVVerifyCompletion completion) {
        }
    };

}