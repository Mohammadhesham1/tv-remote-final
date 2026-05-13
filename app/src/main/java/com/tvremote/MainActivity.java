package com.tvremote;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private RemoteConnection connection;
    private Prefs prefs;
    private Vibrator vibrator;

    private int volume;
    private int channel;
    private boolean muted;
    private boolean powered = true;

    // UI references
    private TextView tvDeviceName;
    private TextView tvStatus;
    private View layoutPoweredOff;
    private View layoutRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        connection = new RemoteConnection(prefs.getTvIp());

        volume = prefs.getVolume();
        channel = prefs.getChannel();
        muted = false;

        bindViews();
        applyPrefs();
        setupButtons();

        // If no TV paired yet, go to pairing screen
        if (prefs.getTvIp().isEmpty()) {
            startActivity(new Intent(this, PairDeviceActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from settings/pairing
        connection.setTvIp(prefs.getTvIp());
        tvDeviceName.setText(prefs.getTvName().isEmpty() ? "التيليفزيون" : prefs.getTvName());
        applyPrefs();
    }

    private void bindViews() {
        tvDeviceName   = findViewById(R.id.tvDeviceName);
        tvStatus       = findViewById(R.id.tvStatus);
        layoutPoweredOff = findViewById(R.id.layoutPoweredOff);
        layoutRemote   = findViewById(R.id.layoutRemote);
    }

    private void applyPrefs() {
        if (prefs.isKeepScreen()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        tvDeviceName.setText(prefs.getTvName().isEmpty() ? "التيليفزيون" : prefs.getTvName());
    }

    private void setupButtons() {
        // Power button
        findViewById(R.id.btnPower).setOnClickListener(v -> {
            vibrate();
            showPowerDialog();
        });

        // Settings
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // Device name / picker
        tvDeviceName.setOnClickListener(v -> {
            vibrate();
            startActivity(new Intent(this, DevicesActivity.class));
        });

        // DPad
        findViewById(R.id.btnUp).setOnClickListener(v -> send(TvCommand.DPAD_UP));
        findViewById(R.id.btnDown).setOnClickListener(v -> send(TvCommand.DPAD_DOWN));
        findViewById(R.id.btnLeft).setOnClickListener(v -> send(TvCommand.DPAD_LEFT));
        findViewById(R.id.btnRight).setOnClickListener(v -> send(TvCommand.DPAD_RIGHT));
        findViewById(R.id.btnOk).setOnClickListener(v -> send(TvCommand.DPAD_OK));

        // Volume
        findViewById(R.id.btnVolUp).setOnClickListener(v -> {
            vibrate();
            if (muted) { muted = false; }
            volume = Math.min(100, volume + 5);
            prefs.setVolume(volume);
            sendWithVal(TvCommand.VOL_UP, volume);
            showToast("صوت: " + volume);
        });
        findViewById(R.id.btnVolDown).setOnClickListener(v -> {
            vibrate();
            volume = Math.max(0, volume - 5);
            prefs.setVolume(volume);
            sendWithVal(TvCommand.VOL_DOWN, volume);
            showToast("صوت: " + volume);
        });
        findViewById(R.id.btnMute).setOnClickListener(v -> {
            vibrate();
            muted = !muted;
            connection.sendUDP(TvCommand.build(TvCommand.MUTE, muted ? 1 : 0));
            showToast(muted ? "صامت" : "صوت مفعّل");
            updateMuteButton();
        });

        // Channel
        findViewById(R.id.btnChUp).setOnClickListener(v -> {
            vibrate();
            channel = Math.min(999, channel + 1);
            prefs.setChannel(channel);
            sendWithVal(TvCommand.CH_UP, channel);
            showToast("القناة: " + channel);
        });
        findViewById(R.id.btnChDown).setOnClickListener(v -> {
            vibrate();
            channel = Math.max(1, channel - 1);
            prefs.setChannel(channel);
            sendWithVal(TvCommand.CH_DOWN, channel);
            showToast("القناة: " + channel);
        });

        // Navigation
        findViewById(R.id.btnHome).setOnClickListener(v -> send(TvCommand.HOME));
        findViewById(R.id.btnBack).setOnClickListener(v -> send(TvCommand.BACK));
        findViewById(R.id.btnMenu).setOnClickListener(v -> send(TvCommand.MENU));

        // Keyboard
        findViewById(R.id.btnKeyboard).setOnClickListener(v -> {
            vibrate();
            showKeyboardDialog();
        });

        // Mic / Voice
        findViewById(R.id.btnMic).setOnClickListener(v -> {
            vibrate();
            showVoiceDialog();
        });

        // App shortcuts
        setupAppButton(R.id.btnNetflix,    "com.netflix.ninja",                  "Netflix");
        setupAppButton(R.id.btnYoutube,    "com.google.android.youtube.tv",       "YouTube");
        setupAppButton(R.id.btnPrime,      "com.amazon.avod.thirdpartyclient",    "Prime Video");
        setupAppButton(R.id.btnDisney,     "com.disney.disneyplus",               "Disney+");
        setupAppButton(R.id.btnAppleTv,    "com.apple.atve.sony.appletv",         "Apple TV");
        setupAppButton(R.id.btnHbo,        "com.hbo.hbonow",                      "HBO Max");

        // Media controls
        findViewById(R.id.btnPlayPause).setOnClickListener(v -> send(TvCommand.PLAY_PAUSE));
        findViewById(R.id.btnRewind).setOnClickListener(v -> send(TvCommand.REWIND));
        findViewById(R.id.btnFastFwd).setOnClickListener(v -> send(TvCommand.FAST_FWD));
        findViewById(R.id.btnPrev).setOnClickListener(v -> send(TvCommand.PREV));
        findViewById(R.id.btnNext).setOnClickListener(v -> send(TvCommand.NEXT));

        // Color buttons
        findViewById(R.id.btnRed).setOnClickListener(v -> { vibrate(); send(TvCommand.BTN_RED); });
        findViewById(R.id.btnGreen).setOnClickListener(v -> { vibrate(); send(TvCommand.BTN_GREEN); });
        findViewById(R.id.btnYellow).setOnClickListener(v -> { vibrate(); send(TvCommand.BTN_YELLOW); });
        findViewById(R.id.btnBlue).setOnClickListener(v -> { vibrate(); send(TvCommand.BTN_BLUE); });

        // Number pad 0-9
        int[] numIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                        R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int i = 0; i < numIds.length; i++) {
            final int num = i;
            findViewById(numIds[i]).setOnClickListener(v -> {
                vibrate();
                connection.sendUDP(TvCommand.build(TvCommand.CH_NUM, num));
                showToast("رقم " + num);
            });
        }

        // Power-off screen "Turn On" button
        findViewById(R.id.btnTurnOn).setOnClickListener(v -> {
            powered = true;
            layoutPoweredOff.setVisibility(View.GONE);
            layoutRemote.setVisibility(View.VISIBLE);
            connection.sendUDP(TvCommand.build(TvCommand.POWER));
            showToast("جارٍ تشغيل التلفزيون...");
        });
    }

    private void setupAppButton(int viewId, String pkg, String appName) {
        View btn = findViewById(viewId);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            vibrate();
            connection.sendUDP(TvCommand.build(TvCommand.OPEN_APP, pkg));
            showToast("جارٍ فتح " + appName);
        });
    }

    private void send(String cmd) {
        vibrate();
        connection.sendUDP(TvCommand.build(cmd));
    }

    private void sendWithVal(String cmd, int val) {
        connection.sendUDP(TvCommand.build(cmd, val));
    }

    private void vibrate() {
        if (!prefs.isHaptic()) return;
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(18);
        }
    }

    private void showToast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private void updateMuteButton() {
        Button btn = findViewById(R.id.btnMute);
        if (btn != null) btn.setText(muted ? "🔇" : "🔊");
    }

    private void showPowerDialog() {
        new AlertDialog.Builder(this)
            .setTitle("إيقاف التلفزيون؟")
            .setMessage("هل تريد إيقاف تشغيل التليفزيون؟")
            .setPositiveButton("إيقاف", (d, w) -> {
                powered = false;
                connection.sendUDP(TvCommand.build(TvCommand.POWER_OFF));
                layoutRemote.setVisibility(View.GONE);
                layoutPoweredOff.setVisibility(View.VISIBLE);
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showKeyboardDialog() {
        final EditText input = new EditText(this);
        input.setHint("اكتب للتليفزيون...");
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
            .setTitle("لوحة المفاتيح")
            .setView(input)
            .setPositiveButton("إرسال", (d, w) -> {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    connection.sendTCP(TvCommand.build(TvCommand.TEXT_INPUT, text), null);
                    showToast("أُرسل: " + text);
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showVoiceDialog() {
        // Show a simple dialog – real voice recognition can be added via SpeechRecognizer
        Toast.makeText(this, "جارٍ الاستماع...", Toast.LENGTH_SHORT).show();
        // TODO: integrate Android SpeechRecognizer and send result via TCP
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) connection.shutdown();
    }
}
