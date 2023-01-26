package com.twilio.twilio_voice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;

import java.util.Objects;


public class AnswerJavaActivity extends AppCompatActivity {

    private static String TAG = "AnswerActivity";
    public static final String TwilioPreferences = "com.twilio.twilio_voicePreferences";

    private NotificationManager notificationManager;
    private boolean isReceiverRegistered = false;
    private VoiceBroadcastReceiver voiceBroadcastReceiver;

    private boolean initiatedDisconnect = false;

    private CallInvite activeCallInvite;
    private int activeCallNotificationId;
    private static final int MIC_PERMISSION_REQUEST_CODE = 17893;
    private PowerManager.WakeLock wakeLock;
    private TextView tvUserName;
    // private TextView tvCallStatus;
    private LinearLayout btnAnswer;
    private LinearLayout btnReject;
    Call.Listener callListener = callListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_answer);

        tvUserName = (TextView) findViewById(R.id.tvUserName);
        // tvCallStatus = (TextView) findViewById(R.id.tvCallStatus);
        btnAnswer = (LinearLayout) findViewById(R.id.btnAnswer);
        btnReject = (LinearLayout) findViewById(R.id.btnReject);

        KeyguardManager kgm = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean isKeyguardUp = kgm.inKeyguardRestrictedInputMode();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        voiceBroadcastReceiver = new VoiceBroadcastReceiver();
        registerReceiver();

        Log.d(TAG, "isKeyguardUp $isKeyguardUp");
        if (isKeyguardUp) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setTurnScreenOn(true);
                setShowWhenLocked(true);
                kgm.requestDismissKeyguard(this, null);
            } else {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                wakeLock.acquire(60 * 1000L /*10 minutes*/);

                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                );
            }

        }

        handleIncomingCallIntent(getIntent());
    }

    private void handleIncomingCallIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "handleIncomingCallIntent-");
            String action = intent.getAction();
            activeCallInvite = intent.getParcelableExtra(Constants.INCOMING_CALL_INVITE);
            activeCallNotificationId = intent.getIntExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, 0);
//            tvCallStatus.setText(R.string.incoming_call_title);
            Log.d(TAG, action);
            switch (action) {
                case Constants.ACTION_INCOMING_CALL:
                case Constants.ACTION_INCOMING_CALL_NOTIFICATION:
                    configCallUI();
                    break;
                case Constants.ACTION_CANCEL_CALL:
                    newCancelCallClickListener();
                    break;
                case Constants.ACTION_ACCEPT:
                    checkPermissionsAndAccept();
                    break;
                case Constants.ACTION_END_CALL:
                    Log.d(TAG, "ending call" + activeCall != null ? "True" : "False");
                    activeCall.disconnect();
                    initiatedDisconnect = true;
                    finishAndRemoveTask();
                    finish();
                    if (activeCall == null) {
                        Log.d(TAG, "No active call to end. Returning");
                        finishAndRemoveTask();
                        break;
                    }

                    break;
                case Constants.ACTION_TOGGLE_MUTE:
                    boolean muted = activeCall.isMuted();
                    activeCall.mute(!muted);
                    break;
                default: {
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent-");
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, intent.getAction());
            if (Constants.ACTION_CANCEL_CALL.equals(intent.getAction())) {
                newCancelCallClickListener();
            }
        }
    }


    private void configCallUI() {
        Log.d(TAG, "configCallUI");
        if (activeCallInvite != null) {

//            String fromId = Objects.requireNonNull(activeCallInvite.getFrom()).replace("client:", "");
//            SharedPreferences preferences = getApplicationContext().getSharedPreferences(TwilioPreferences, Context.MODE_PRIVATE);
//            String caller = preferences.getString(fromId, preferences.getString("defaultCaller", getString(R.string.unknown_caller)));
//            tvUserName.setText(caller);
            String firstname = activeCallInvite.getCustomParameters().get("firstname");
            String lastname = activeCallInvite.getCustomParameters().get("lastname");
            String phoneNum = activeCallInvite.getFrom();
            Log.d(TAG,firstname);
            Log.d(TAG,lastname);
            Log.d(TAG,phoneNum);

            String allNameUsed =
                    (firstname == null || firstname.isEmpty())  && (lastname == null || lastname.isEmpty()) ?
                      phoneNum :firstname +" "+ lastname;


            tvUserName.setText(allNameUsed);


//            btnAnswer.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Log.d(TAG, "onCLick");
//                    checkPermissionsAndAccept();
//                }
//            });
//
//            btnReject.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    rejectCallClickListener();
//                }
//            });
            btnAnswer.setOnClickListener(v -> {
                Log.d(TAG, "click: Call Accepted");
                AnswerJavaActivity.this.checkPermissionsAndAccept();
            });

            btnReject.setOnClickListener(v -> AnswerJavaActivity.this.rejectCallClickListener());
        }
    }

    private void checkPermissionsAndAccept() {
        Log.d(TAG, "Clicked accept");
        if (!checkPermissionForMicrophone()) {
            Log.d(TAG, "configCallUI-requestAudioPermissions");
            requestAudioPermissions();
        } else {
            Log.d(TAG, "configCallUI-newAnswerCallClickListener");
            acceptCall();
        }
    }


    private void acceptCall() {
        Log.d(TAG, "Accepting call");
        Intent acceptIntent = new Intent(this, IncomingCallNotificationService.class);
        acceptIntent.setAction(Constants.ACTION_ACCEPT);
        acceptIntent.putExtra(Constants.INCOMING_CALL_INVITE, activeCallInvite);
        acceptIntent.putExtra(Constants.ACCEPT_CALL_ORIGIN, 1);
        acceptIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, activeCallNotificationId);
        Log.d(TAG, "Clicked accept startService");
        startService(acceptIntent);
        Log.d(TAG, "isLocked: " + isLocked() + " appHasStarted: " + TwilioVoicePlugin.appHasStarted);
        if (TwilioVoicePlugin.appHasStarted) {
            Log.d(TAG, "AnswerJavaActivity Finish");
            finish();
        }
        else {
            Log.d(TAG, "Answering call");
            activeCallInvite.accept(this, callListener);
            notificationManager.cancel(activeCallNotificationId);
            ///finish();
        }
    }

    private boolean isLocked() {
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    private void startAnswerActivity(Call call) {
        Intent intent = new Intent(this, BackgroundCallJavaActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String firstname = activeCallInvite.getCustomParameters().get("firstname");
        String lastname = activeCallInvite.getCustomParameters().get("lastname");
        String phoneNum = activeCallInvite.getFrom();
        String allNameUsed =
                (firstname == null || firstname.isEmpty())  && (lastname == null || lastname.isEmpty()) ?
                        phoneNum :firstname +" "+ lastname;
        intent.putExtra(Constants.CALL_FROM, allNameUsed);
        startActivity(intent);
        finish();
        Log.d(TAG, "Connected");

    }

    private void endCall() {
        Log.d(TAG, "endCall - initiatedDisconnect: " + initiatedDisconnect);
        if (!initiatedDisconnect) {
            Intent intent = new Intent(this, BackgroundCallJavaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Constants.ACTION_CANCEL_CALL);

            this.startActivity(intent);
            finishAndRemoveTask();
            finish();
        }

    }

    Call activeCall;

    private Call.Listener callListener() {
        return new Call.Listener() {


            @Override
            public void onConnectFailure(@NonNull Call call, @NonNull CallException error) {
                Log.d(TAG, "Connect failure");
                Log.e(TAG, "Call Error: %d, %s" + error.getErrorCode() + error.getMessage());
            }

            @Override
            public void onRinging(@NonNull Call call) {

            }

            @SuppressLint("SuspiciousIndentation")
            @Override
            public void onConnected(@NonNull Call call) {
                activeCall = call;
//                if (!TwilioVoicePlugin.appHasStarted) {
                    Log.d(TAG, "Connected from BackgroundUI");
                    startAnswerActivity(call);
               // }
            }

            @Override
            public void onReconnecting(@NonNull Call call, @NonNull CallException callException) {
                Log.d(TAG, "onReconnecting");
            }

            @Override
            public void onReconnected(@NonNull Call call) {
                Log.d(TAG, "onReconnected");
            }

            @Override
            public void onDisconnected(@NonNull Call call, CallException error) {
                if (!TwilioVoicePlugin.appHasStarted) {
                    Log.d(TAG, "Disconnected");
                    endCall();
                }
            }

        };
    }

    private class VoiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received broadcast for action " + action);

            if (action != null)
                switch (action) {
                    case Constants.ACTION_INCOMING_CALL:
                    case Constants.ACTION_CANCEL_CALL:
                    case Constants.ACTION_TOGGLE_MUTE:
                    case Constants.ACTION_END_CALL:
                        /*
                         * Handle the incoming or cancelled call invite
                         */
                        Log.d(TAG, "received intent to answerActivity");
                        handleIncomingCallIntent(intent);
                        break;
                    default:
                        Log.d(TAG, "Received broadcast for other action " + action);
                        break;

                }
        }
    }

    private void registerReceiver() {
        Log.d(TAG, "Registering answerJavaActivity receiver");
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_TOGGLE_MUTE);
            intentFilter.addAction(Constants.ACTION_CANCEL_CALL);
            intentFilter.addAction(Constants.ACTION_END_CALL);
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    voiceBroadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        Log.d(TAG, "Unregistering receiver");
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    // We still want to listen messages from backgroundCallJavaActivity
//    @Override
//    protected void onPause() {
//        super.onPause();
//        unregisterReceiver();
//    }

    private void newCancelCallClickListener() {
        finish();
    }

    private void rejectCallClickListener() {
        Log.d(TAG, "Reject Call Click listener");
        if (activeCallInvite != null) {
            Intent rejectIntent = new Intent(this, IncomingCallNotificationService.class);
            rejectIntent.setAction(Constants.ACTION_REJECT);
            rejectIntent.putExtra(Constants.INCOMING_CALL_INVITE, activeCallInvite);
            startService(rejectIntent);
            finish();
        }
    }

    private Boolean checkPermissionForMicrophone() {
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermissions() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        Log.d(TAG, "requestAudioPermissions");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(this, permissions, MIC_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, permissions, MIC_PERMISSION_REQUEST_CODE);
            }
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestAudioPermissions-> permission granted->newAnswerCallClickListener");
            acceptCall();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permissions needed. Please allow in your application settings.", Toast.LENGTH_LONG).show();
                rejectCallClickListener();
            } else {
                acceptCall();
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "AnwserJAvaActivity ondestroy");
        super.onDestroy();
        unregisterReceiver();
        if (wakeLock != null) {
            wakeLock.release();
        }
    }

}
