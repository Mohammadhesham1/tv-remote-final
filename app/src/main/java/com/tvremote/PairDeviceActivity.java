package com.tvremote;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class PairDeviceActivity extends AppCompatActivity {

    private Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);

        prefs = new Prefs(this);

        findViewById(R.id.btnScanDevices).setOnClickListener(v -> {
            startActivity(new Intent(this, DevicesActivity.class));
        });

        findViewById(R.id.btnManualPair).setOnClickListener(v -> {
            EditText etIp = findViewById(R.id.etIpAddress);
            String ip = etIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, "أدخل عنوان IP", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.setTvIp(ip);
            prefs.setTvName("التيليفزيون");
            Toast.makeText(this, "تم الحفظ: " + ip, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        findViewById(R.id.btnSkip).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
