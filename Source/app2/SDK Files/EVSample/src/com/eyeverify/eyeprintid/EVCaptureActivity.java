package com.eyeverify.eyeprintid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eyeverify.evserviceinterface.aidl.data.EVServiceHelper;
import com.eyeverify.evserviceinterface.client.EVEnrollCompletion;
import com.eyeverify.evserviceinterface.client.EVRegisterCompletion;
import com.eyeverify.evserviceinterface.client.EVServiceBusyException;
import com.eyeverify.evserviceinterface.client.EVServiceClient;
import com.eyeverify.evserviceinterface.client.EVServiceException;
import com.eyeverify.evserviceinterface.client.EVServiceListener;
import com.eyeverify.evserviceinterface.client.EVVerifyCompletion;
import com.eyeverify.evserviceinterface.client.base.EVServiceProperties;
import com.eyeverify.evserviceinterface.client.event.EVEyeRegionsChangedEvent;
import com.eyeverify.evserviceinterface.client.event.EVEyeRegionsChangedListener;
import com.eyeverify.evserviceinterface.client.event.EVEyeStatusChangedEvent;
import com.eyeverify.evserviceinterface.client.event.EVEyeStatusChangedListener;
import com.eyeverify.evserviceinterface.constants.EVEnums;
import com.eyeverify.evserviceinterface.constants.EVEvents;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

public class EVCaptureActivity extends BaseActivity {

    public static final String TAG = EVCaptureActivity.class.getSimpleName();

    public static final String SERVICE_PACKAGE_KEY = "SERVICE_PACKAGE";
    public static final String IS_ENROLLMENT_KEY = "IS_ENROLLMENT";
    public static final String USER_ID_KEY = "USER_ID";
    public static final String USERKEY_KEY = "USER_KEY";

    private enum MESSAGE_STATE { ALERT, NEW_SESSION, ERROR, ABORT}

    private static final String SHARED_PREFERENCES = "EVServiceSampleActivity-SharedPreferences";
    private static final String PUBLIC_KEY_PREFERENCE = "publicKey";

    private EVServiceClient mServiceClient;
    private byte[] mNonce;
    boolean isEnrollment = false;
    String userID;
    String userKey;

    boolean isMidSession;
    boolean hasLaunched;

    private ViewGroup service_window;

    ProgressBar enroll_progress;
    Button scan_again_button;
    Button continue_button;
    Button cancel_button;
    Button login_button;
    Button my_button;
    ImageView capture_complete_checkmark;

    ViewGroup service_overlay;

    int overlay_width;
    int overlay_height;
    int overlay_top;
    int overlay_left;

    private View leftEyeBox;
    private View rightEyeBox;

    TargetRectangle target_box;
    TextView counter_text;
   TextView capture_notification_text;
    TextView large_notification_text;
    TextView sub_notification_text;

    String servicePackageName;

    int mStep, mTotalSteps, mCounter, killSwitchTimeOut;

    Handler mIdleTimer;
    Runnable mIdleRunnable;

    private EVEnums.EyeStatus currentEyeStatus = EVEnums.EyeStatus.None;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_capture);

        service_window = (ViewGroup) findViewById(R.id.capture_window);

        mServiceClient = new EVServiceClient(mListener, new EVServiceProperties(BaseActivity.readLicenseCertificate()));

        Intent intent = getIntent();

        intent.getBooleanExtra(IS_ENROLLMENT_KEY, false);

        servicePackageName = intent.getStringExtra(SERVICE_PACKAGE_KEY);
        if (servicePackageName == null) {
            servicePackageName = getBaseContext().getPackageName();
        }
        isEnrollment = intent.getBooleanExtra(IS_ENROLLMENT_KEY, false);
        userID = intent.getStringExtra(USER_ID_KEY);
        userKey = intent.getStringExtra(USERKEY_KEY);

        mIdleRunnable = new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };

        enroll_progress = (ProgressBar) findViewById(R.id.capture_enroll_progress);

        capture_complete_checkmark =  (ImageView) findViewById(R.id.capture_complete_checkmark);

        scan_again_button = (Button) findViewById(R.id.capture_scan_again_button);
        scan_again_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    reconfigureProgressBar();
                    mServiceClient.continueAuth();
                    resumeAuth();
                } catch (Throwable ex) {
                    String msg = "Failed to continue.";
                    Log.e(TAG, msg, ex);
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            }
        });



        cancel_button = (Button) findViewById(R.id.capture_cancel_button);
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeVideoOverlays();

                finish();

            }
        });
        cancel_button.setEnabled(false);

        continue_button = (Button) findViewById(R.id.capture_continue_button);

        continue_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeVideoOverlays();
                finish();
            }
        });

        service_overlay = (ViewGroup) findViewById(R.id.capture_overlay);

        target_box = (TargetRectangle) findViewById(R.id.capture_target_box);
        counter_text = (TextView) findViewById(R.id.capture_counter_text);

       capture_notification_text = (TextView) findViewById(R.id.capture_notification_text);
      //  TextView t1=(TextView)findViewById(R.id.capture_notification_text);



        large_notification_text = (TextView) findViewById(R.id.capture_large_notification_text);
        sub_notification_text = (TextView) findViewById(R.id.capture_sub_notification_text);

//        leftEyeBox =  findViewById(R.id.left_eye_box);
//        rightEyeBox =  findViewById(R.id.right_eye_box);
        my_button=(Button)findViewById(R.id.meena_button);
        my_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent k=new Intent(EVCaptureActivity.this,InstructionsActivity.class);
                startActivity(k);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        configureProgressBar(0, 10, 0);

        scan_again_button.setVisibility(View.GONE);
        continue_button.setVisibility(View.GONE);
        my_button.setVisibility(View.GONE);
        capture_complete_checkmark.setVisibility(View.GONE);

        if (isMidSession) {
            Toast.makeText(getApplicationContext(), R.string.capture_closed_incomplete_message, Toast.LENGTH_LONG).show();
            finish();
        } else if (!hasLaunched) {
            //must occur after window is available
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        hasLaunched = true;
                        mServiceClient.connect(servicePackageName, getBaseContext(), service_window);

                    } catch (Throwable ex) {
                        String msg = "Failed to connect.";
                        Log.e(TAG, msg, ex);
                    }
                }
            }, 100);
        } else {
            finish();//launched and finished, but maybe a phone call afterwards while sitting on finish screen
        }
    }

    private void doAuth() {
        try {
            cancel_button.setEnabled(true);
            Log.d(TAG, "EVCaptureActivity.doAuth");

            if (isEnrollment) {
                mServiceClient.enrollUser(userID, userKey.getBytes());
            } else {
                mNonce = EVServiceHelper.generate(8).getBytes();
                mServiceClient.verifyUser(userID, mNonce);
            }

            killSwitchTimeOut = mServiceClient.getSetting("kill_switch_abort_timeout", Integer.class);

        } catch (EVServiceException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (EVServiceBusyException e) {
            Toast.makeText(getApplicationContext(), "Cannot continue, currently busy", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startInactivityTimer() {

        mIdleTimer = new Handler();
        mIdleTimer.postDelayed(mIdleRunnable, 1000 * (killSwitchTimeOut != 0? killSwitchTimeOut : 180));
    }

    private void cancelInactivityTimer() {
        if (mIdleTimer != null) {
            mIdleTimer.removeCallbacks(mIdleRunnable);
        }
    }

    protected void onPause() {
        removeVideoOverlays();

        cancelInactivityTimer();

        try {
            mServiceClient.releaseCamera();
            mServiceClient.disconnect();
        } catch (Throwable ex) {
            String msg = "Failed to disconnect.";
            Log.e(TAG, msg, ex);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        removeVideoOverlays();
        super.onDestroy();
    }

    private synchronized void addVideoOverlays() {

        if (0 == overlay_width) {
            overlay_width = service_overlay.getWidth();
            overlay_height = service_overlay.getHeight();
            overlay_top = service_overlay.getTop();
            overlay_left = service_overlay.getLeft();

            ((ViewGroup)service_overlay.getParent()).removeView(service_overlay);
        }

        WindowManager windowManager = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(overlay_width, overlay_height, overlay_left, overlay_top, WindowManager.LayoutParams.TYPE_TOAST, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.RGBA_8888 );
        params.gravity = Gravity.TOP | Gravity.CENTER;

        try {
            windowManager.addView(service_overlay, params);
        } catch (Exception e) {
            //already added
        }

        service_overlay.setVisibility(View.VISIBLE);

        target_box.setTargetSuccess(false);
        target_box.startScanning();
    }

    private synchronized void removeVideoOverlays() {

        WindowManager windowManager = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);

        try {
            windowManager.removeViewImmediate(service_overlay);
        } catch ( Exception e) { }
    }

    void reconfigureProgressBar() {
        configureProgressBar(mStep, mTotalSteps, mCounter);
    }

    private void configureProgressBar(int step, int totalSteps, int counter) {

        mStep = step;
        mTotalSteps = totalSteps;
        mCounter = counter;

        enroll_progress.setMax(100);

        int value = 0;

        if (step<=0) {
            value = 0;
        } else if (step>=totalSteps) {
            value = enroll_progress.getMax();
        } else {
            value = step * enroll_progress.getMax() / totalSteps;
        }

        enroll_progress.setProgress(value);

        if (value == enroll_progress.getMax()) {
            capture_notification_text.setTextColor(getResources().getColor(R.color.notification_text_color_complete));
        } else {
            capture_notification_text.setTextColor(getResources().getColor(R.color.notification_text_color));
        }

        if (counter < 1 || value == enroll_progress.getMax()) {
            counter_text.setVisibility(View.GONE);
        } else {
            counter_text.setVisibility(View.VISIBLE);
            counter_text.setText("" + counter);
        }
    }

    private void clearAllTexts() {
        capture_notification_text.setText("");
        large_notification_text.setText("");
        sub_notification_text.setText("");
    }

    private void resumeAuth() {
        clearAllTexts();
        target_box.startScanning();
        counter_text.setVisibility(View.VISIBLE);
        scan_again_button.setVisibility(View.GONE);
    }

    public void checkCameraPermission()  {

        boolean granted = false;
        if (mServiceClient != null) {
            try {
                mServiceClient.getPermissionInfo();
            } catch (EVServiceException ex) {
                mPermissionState = BaseActivity.kPERMISSION_STATE_UNKNOWN;
                return;
            }
        }

        if (granted)
            mPermissionState = BaseActivity.kPERMISSION_STATE_GRANTED;
        else
            mPermissionState = BaseActivity.kPERMISSION_STATE_REVOKED;

    }

    private class EVListener implements EVServiceListener, EVEyeRegionsChangedListener, EVEyeStatusChangedListener {

        @Override
        public void registrationCompleted(EVRegisterCompletion completion) {

        }

        @Override
        public void onServiceAvailable() {
            doAuth();
            checkCameraPermission();
        }

        @Override
        public void onWindowAdded() {
            addVideoOverlays();
        }

        @Override
        public void onWindowRemoved() {
            //do something probably
            removeVideoOverlays();
        }

        @Override
        public void onWindowFailure() {
            Toast.makeText(getApplicationContext(), "Camera unavailable. Please restart the device.", Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void handleEvent(EVEyeRegionsChangedEvent event) {
            // Log.d(TAG, "Handle EVEyeRegionsChangedEvent: previewLeftX=" + event.getOriginalPreviewLeftX() + "; previewLeftY=" + event.getOriginalPreviewLeftY() + "; previewLeftW=" + event.getOriginalPreviewLeftW() + "; previewLeftH=" + event.getOriginalPreviewLeftH());

            if (currentEyeStatus==EVEnums.EyeStatus.None || leftEyeBox==null || rightEyeBox==null) {
                return;
            }

            leftEyeBox.setVisibility(View.VISIBLE);
            rightEyeBox.setVisibility(View.VISIBLE);

            Integer leftX = event.getPreviewLeftX();
            Integer leftY = event.getPreviewLeftY();
            Integer leftW = event.getPreviewLeftW();
            Integer leftH = event.getPreviewLeftH();

            Integer rightX = event.getPreviewRightX();
            Integer rightY = event.getPreviewRightY();
            Integer rightW = event.getPreviewRightW();
            Integer rightH = event.getPreviewRightH();

            if (leftH!=null && leftW!=null && leftX!=null && leftY!=null
                    && rightX!=null && rightY!=null && rightW!=null && rightH!=null) {

                int screenW = service_window.getWidth();
                int screenH = service_window.getHeight();

                RelativeLayout.LayoutParams leftLayoutParams = new RelativeLayout.LayoutParams(service_window.getLayoutParams());
                leftLayoutParams.setMargins(leftX, leftY, screenW - (leftX + leftW), screenH - (leftY + leftH));
                leftEyeBox.setLayoutParams(leftLayoutParams);

                RelativeLayout.LayoutParams rightLayoutParams = new RelativeLayout.LayoutParams(service_window.getLayoutParams());
                rightLayoutParams.setMargins(rightX, rightY, screenW - (rightX + rightW), screenH - (rightY + rightH));
                rightEyeBox.setLayoutParams(rightLayoutParams);
            }
        }

        @Override
        public void handleEvent(EVEyeStatusChangedEvent event) {
            currentEyeStatus = event.getEyeStatus();
            Log.d(TAG, "Handle EVEyeStatusChangedEvent: status=" + currentEyeStatus);

            if (currentEyeStatus == null) return;

            target_box.setTargetSuccess(false);

            switch (currentEyeStatus) {
                case Okay:
                    target_box.setTargetSuccess(true);
                    capture_notification_text.setText(null);
                    break;
                case NoEye:
                    capture_notification_text.setText(getString(R.string.capture_message_align));
                    break;
                case TooClose:
                case TooFarAway:
                    capture_notification_text.setText(getString(R.string.capture_message_distance));
                    break;
                case NoGaze:
                    break;
                default:
                    break;
            }
            cancelInactivityTimer();
        }

        @Override
        public void handleEvent(int eventCode, Map<String, String> params) {

            switch (eventCode) {
                case EVEvents.DispatchEvent_EnrollmentStarted: {
                    clearAllTexts();

                    isMidSession = true;
                    break;
                }
                case EVEvents.DispatchEvent_EnrollmentSessionStarted: {
                    int counter = EVEvents.getParameter(EVEvents.kEnrollmentCounter, params, Integer.class);
                    counter_text.setText(counter + "");

                    resumeAuth();

                    cancelInactivityTimer();
                    break;
                }
                case EVEvents.DispatchEvent_EnrollmentSessionCompleted: {

                    boolean enrollmentEnding = EVEvents.getParameter(EVEvents.kEnrollmentEnding, params, Boolean.class);

                    if (!enrollmentEnding) {
                        changeMessageState(MESSAGE_STATE.NEW_SESSION);
                    }

                    counter_text.setVisibility(View.GONE);

                    target_box.setTargetSuccess(false);
                    target_box.stopScanning();

                    startInactivityTimer();
                    break;
                }
                case EVEvents.DispatchEvent_EnrollmentStepCompleted: {

                    int step = EVEvents.getParameter(EVEvents.kEnrollmentStepCompleted, params, Integer.class);
                    int totalSteps = EVEvents.getParameter(EVEvents.kEnrollmentTotalSteps, params, Integer.class);
                    int counter = EVEvents.getParameter(EVEvents.kEnrollmentCounter, params, Integer.class);

                    configureProgressBar(step, totalSteps, counter);

                    break;
                }
                case EVEvents.DispatchEvent_EnrollmentCompleted: {
                    counter_text.setVisibility(View.GONE);
                    isMidSession = false;

                    cancelInactivityTimer();
                    break;
                }
                case EVEvents.DispatchEvent_VerificationStarted: {
                    clearAllTexts();
                    isMidSession = true;
                    configureProgressBar(0, 10, 0);

                    cancelInactivityTimer();
                    break;
                }
                case EVEvents.DispatchEvent_VerificationCompleted: {
                    target_box.stopScanning();
                    isMidSession = false;

                    startInactivityTimer();
                    break;
                }
                default:

            }
        }

        @Override
        public void enrollmentCompleted(EVEnrollCompletion completion) {
            try {

                scan_again_button.setVisibility(View.GONE);
                cancel_button.setVisibility(View.GONE);
                continue_button.setVisibility(View.VISIBLE);
                my_button.setVisibility(View.GONE);


                Log.d(TAG, "Starting enrollmentCompleted: success=" + completion.isSuccess() + "; user_key=" + new String(completion.getUserKey()));

                //Toast.makeText(getApplicationContext(), completion.isSuccess() ? R.string.main_enrollment_success : R.string.main_enrollment_failure, Toast.LENGTH_LONG).show();

                if (completion.isSuccess()) {
                    Log.d(TAG, "Storing public key: encodedPublicKey=" + EVServiceHelper.data2string(completion.getEncodedPublicKey()));

                    SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES, Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    if (completion.getEncodedPublicKey() != null && completion.getEncodedPublicKey().length > 0) {
                        editor.putString(PUBLIC_KEY_PREFERENCE, EVServiceHelper.data2string(completion.getEncodedPublicKey()));
                    } else {
                        editor.remove(PUBLIC_KEY_PREFERENCE);
                    }
                    editor.apply();

                    capture_complete_checkmark.setVisibility(View.VISIBLE);
                }

                if (completion.isSuccess()) {
                    capture_notification_text.setText(getString(R.string.enroll_completed_message));
                    sub_notification_text.setText(getString(R.string.enroll_done_message));
                    large_notification_text.setVisibility(View.GONE);
                    sub_notification_text.setVisibility(View.GONE);
                    return;
                }
                else if (completion.isIncomplete()) {
                    capture_notification_text.setText(getString(R.string.enroll_incomplete_message));
                    sub_notification_text.setText(getString(R.string.enroll_retry_message));
                    return;
                }
                else if (completion.wasAborted()) {
                    if (SharedGlobals.isLicensingError(completion.getAbortResult())) {
                        BaseActivity.deleteLicenseCertificate();
                        Intent groupIdActivity = new Intent(EVCaptureActivity.this, GroupIdActivity.class);
                        groupIdActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        groupIdActivity.putExtra(SharedGlobals.LICENSE_ERROR, completion.getAbortResult());
                        startActivity(groupIdActivity);
                        finish();
                    }

                    showAbortMessages(completion.getAbortResult());
                    return;
                }

                switch (completion.getEnrollmentResult()) {
                    case bad_quality:
                        showError(SharedGlobals.ERROR_TYPE.QUALITY);
                        break;
                    case error:
                        showError(SharedGlobals.ERROR_TYPE.SYSTEM);
                        break;
                    case http_error:
                        showError(SharedGlobals.ERROR_TYPE.INTERNET);
                        break;
                    case bad_match:
                        showError(SharedGlobals.ERROR_TYPE.ENROLLMENT_MATCH);
                        break;
                    case zero_images:
                    case no_eyes:
                        showError(SharedGlobals.ERROR_TYPE.NO_EYE);
                        break;
                    case low_lighting:
                        showError(SharedGlobals.ERROR_TYPE.LOW_LIGHTING);
                        break;
                    default:
                        showError(SharedGlobals.ERROR_TYPE.NOT_ENROLLED); //show default error message
                        break;
                }

                Log.d(TAG, "Finished enrollmentCompleted: success="+ completion.isSuccess());
            } catch (Throwable ex) {
                Log.e(TAG, "Failed to complete enrollment.", ex);
            }
        }

        @Override
        public void verificationCompleted(EVVerifyCompletion completion) {
            try {

                scan_again_button.setVisibility(View.GONE);
                cancel_button.setVisibility(View.GONE);
                continue_button.setVisibility(View.VISIBLE);
                my_button.setVisibility(View.VISIBLE);




             //   finish();
                //Intent i=new Intent(EVCaptureActivity.this,SettingActivity.class);
           /*     String h="ok";
                i.putExtra("value",h);
                startService(i);*/
                // h="not ok";
                Log.d(TAG, "Starting verificationCompleted: success=" + completion.isSuccess() + "; user_key=" + new String(completion.getUserKey()));

                //Toast.makeText(getApplicationContext(), completion.isSuccess() ? R.string.main_verify_success : R.string.main_verify_failure, Toast.LENGTH_LONG).show();

                boolean signatureVerify = false;
                if (completion.isSuccess()) {

                    if (isAudioEnabled()) {
                        MediaPlayer verifiedPromptMediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.verified_prompt);
                        verifiedPromptMediaPlayer.start();

                    }

                    if (completion.getSignedNonce() != null && completion.getSignedNonce().length>0) {
                        Log.d(TAG, "Verifying signature: mNonce="+ EVServiceHelper.data2string(mNonce)+"; signedNonce=" + EVServiceHelper.data2string(completion.getSignedNonce()));

                        SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES, Activity.MODE_PRIVATE);
                        byte[] encodedPublicKey = EVServiceHelper.string2data(prefs.getString(PUBLIC_KEY_PREFERENCE, null));
                        if (encodedPublicKey == null || encodedPublicKey.length == 0) {
                            Log.w(TAG, "Missing public key. Please register first.");
                            return;
                        }

                        X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedPublicKey);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        PublicKey publicKey = kf.generatePublic(spec);

                        Signature instance = Signature.getInstance("SHA1withRSA");
                        instance.initVerify(publicKey);
                        instance.update(mNonce);

                        signatureVerify = instance.verify(completion.getSignedNonce());
                    }
                    else {
                        Log.d(TAG, "Skipped signature verify because registrtaion is disabled on the service side.");
                    }

                    capture_notification_text.setText(getString(R.string.main_verify_success));

                    large_notification_text.setVisibility(View.GONE);
                    sub_notification_text.setVisibility(View.GONE);
                    capture_complete_checkmark.setVisibility(View.VISIBLE);
                    login_button.setVisibility(View.VISIBLE);

                }
                else if (completion.wasAborted()) {
                    showAbortMessages(completion.getAbortResult());
                    return;
                }
                else {

                    switch (completion.getVerificationResult()) {
                        case bad_quality:
                            showError(SharedGlobals.ERROR_TYPE.QUALITY);
                            break;
                        case error:
                            showError(SharedGlobals.ERROR_TYPE.SYSTEM);
                            break;
                        case http_error:
                            showError(SharedGlobals.ERROR_TYPE.INTERNET);
                            break;
                        case zero_images:
                            showError(SharedGlobals.ERROR_TYPE.NO_EYE);
                            break;
                        default:
                            showError(SharedGlobals.ERROR_TYPE.NOT_VERIFIED); //show default error message
                            break;
                    }

                }


                Log.d(TAG, "Finished verifyCompleted: success="+ completion.isSuccess()+"; signatureVerify="+signatureVerify);
            } catch (Throwable ex) {
                Log.e(TAG, "Failed to complete verification.", ex);
            }
        }
    };
    private EVServiceListener mListener = new EVListener();

    private void showError(SharedGlobals.ERROR_TYPE theError) {
        MESSAGE_STATE message_state = MESSAGE_STATE.ERROR;

        if (large_notification_text == null) return;
        large_notification_text.setTextColor(getResources().getColor(R.color.ev_heading_text_error));
        large_notification_text.setVisibility(View.VISIBLE);
        sub_notification_text.setVisibility(View.VISIBLE);
        login_button.setVisibility(View.GONE);

        switch (theError) {
            case NOT_ENROLLED:
                large_notification_text.setText(getString(R.string.capture_error_not_enrolled));
                sub_notification_text.setText(getString(R.string.capture_suggestion_wipe_camera));
                break;
            case DISTANCE:
                large_notification_text.setText(getString(R.string.capture_error_distance));
                sub_notification_text.setText(getString(R.string.capture_suggestion_distance));
                break;
            case NO_EYE:
                large_notification_text.setText(getString(R.string.capture_noeye_error));
                sub_notification_text.setText(getString(R.string.capture_suggestion_no_eye));
                break;
            case QUALITY:
                large_notification_text.setText(getString(R.string.capture_error_quality));
                sub_notification_text.setText(getString(R.string.capture_suggestion_quality));
                break;
            case SYSTEM:
                large_notification_text.setText(getString(R.string.capture_error_system));
                sub_notification_text.setText(getString(R.string.capture_suggestion_system));
                break;
            case LICENSE_INVALID:
                message_state = MESSAGE_STATE.ABORT;
                large_notification_text.setText(getString(R.string.capture_license_invalid_suggestion));
                sub_notification_text.setText("");
                break;
            case LICENSE_EXPIRED:
                message_state = MESSAGE_STATE.ABORT;
                large_notification_text.setText(getString(R.string.capture_license_expired_suggestion));
                sub_notification_text.setText("");
                break;
            case LICENSE_LIMITED:
                message_state = MESSAGE_STATE.ABORT;
                large_notification_text.setText(getString(R.string.capture_license_limited_suggestion));
                sub_notification_text.setText("");
                break;
            case INTERNET:
                message_state = MESSAGE_STATE.ABORT;
                large_notification_text.setText(getString(R.string.capture_error_internet));
                sub_notification_text.setText(getString(R.string.capture_suggestion_internet));
                break;
            case ENROLLMENT_MATCH:
                large_notification_text.setText(getString(R.string.capture_error_enrollment_match));
                sub_notification_text.setText(getString(R.string.capture_suggestion_enrollment_match));
                break;
            case CHAFF:
                large_notification_text.setText(getString(R.string.capture_error_chaff));
                sub_notification_text.setText(getString(R.string.capture_suggestion_chaff));
                break;
            case APP_BACKGROUND:
                large_notification_text.setText(getString(R.string.capture_error_background));
                sub_notification_text.setText(getString(R.string.capture_suggestion_background));
                break;
            case LOW_LIGHTING:
                large_notification_text.setText(getString(R.string.capture_error_low_lighting));
                sub_notification_text.setText(getString(R.string.capture_suggestion_low_lighting));
                break;
            case NOT_SUPPORTED:
                message_state = MESSAGE_STATE.ABORT;
                large_notification_text.setText(getString(R.string.capture_error_not_supported));
                sub_notification_text.setText(getString(R.string.capture_suggestion_not_supported));
                break;
            case CAMERA_EXCEPTION:
                message_state = MESSAGE_STATE.ABORT;
                large_notification_text.setText("The camera has experienced an error.");
                sub_notification_text.setText("EyeVerify will now close");
            default:
                large_notification_text.setText(mServiceClient.isEnrollment() ? getString(R.string.capture_error_not_enrolled) : getString(R.string.capture_error_not_verified));
                sub_notification_text.setText(getString(R.string.capture_suggestion_wipe_camera));
                break;
        }

        changeMessageState(message_state);
    }

    private void changeMessageState(MESSAGE_STATE messageState) {

        large_notification_text.setVisibility(View.INVISIBLE);
        sub_notification_text.setVisibility(View.INVISIBLE);

        counter_text.setVisibility(View.GONE);
        scan_again_button.setVisibility(View.GONE);

        //only change what differs from defaults above
        switch (messageState) {
            case ALERT:
                scan_again_button.setVisibility(View.VISIBLE);

                break;
            case NEW_SESSION:

                large_notification_text.setVisibility(View.VISIBLE);

                if (enroll_progress.getProgress() < 50) {
                    //TODO: get last session as event
                    large_notification_text.setText(getString(R.string.capture_enrollment_new_session));
                } else {
                    large_notification_text.setText(getString(R.string.capture_enrollment_new_session_almost_done));
                }

                sub_notification_text.setVisibility(View.VISIBLE);
                sub_notification_text.setText(getString(R.string.capture_suggestion_enrollment_new_session));

                scan_again_button.setVisibility(View.VISIBLE);
                scan_again_button.setText(getString(R.string.button_title_scan_again));

                break;
            case ERROR:
                large_notification_text.setVisibility(View.VISIBLE);
                sub_notification_text.setVisibility(View.VISIBLE);

                scan_again_button.setText(getString(R.string.button_title_keep_scanning));
                scan_again_button.setVisibility(View.VISIBLE);

                break;
            case ABORT:
                large_notification_text.setVisibility(View.VISIBLE);
                sub_notification_text.setVisibility(View.VISIBLE);

                break;
            default:
        }
    }

    private void showAbortMessages(EVEnums.abort_reason abortReason) {

        switch(abortReason) {
            case license_invalid:
                showError(SharedGlobals.ERROR_TYPE.LICENSE_INVALID);
                break;
            case license_expired:
                showError(SharedGlobals.ERROR_TYPE.LICENSE_EXPIRED);
                break;
            case license_limited:
                showError(SharedGlobals.ERROR_TYPE.LICENSE_LIMITED);
                break;
            case internet_required:
                showError(SharedGlobals.ERROR_TYPE.INTERNET);
                break;
            case app_background:
                showError(SharedGlobals.ERROR_TYPE.APP_BACKGROUND);
                break;
            case abort_low_lighting:
                showError(SharedGlobals.ERROR_TYPE.LOW_LIGHTING);
                break;
            case unsupported_device:
                showError(SharedGlobals.ERROR_TYPE.NOT_SUPPORTED);
                break;
            case system_timeout:
                showError(SharedGlobals.ERROR_TYPE.NO_EYE);
                break;
            case abort_camera_exception:
                showError(SharedGlobals.ERROR_TYPE.CAMERA_EXCEPTION);
            default: {
                switch (currentEyeStatus) {
                    case NoEye:
                        showError(SharedGlobals.ERROR_TYPE.NO_EYE);
                        break;
                    case TooClose:
                    case TooFarAway:
                        showError(SharedGlobals.ERROR_TYPE.DISTANCE);
                        break;
                    default:
                        showError(mServiceClient.isEnrollment() ? SharedGlobals.ERROR_TYPE.NOT_ENROLLED : SharedGlobals.ERROR_TYPE.NOT_VERIFIED);
                        break;
                }
                break;
            }
        }
    }
}
