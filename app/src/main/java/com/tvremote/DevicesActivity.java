package com.tvremote;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DevicesActivity extends AppCompatActivity {

    private Prefs prefs;
    private RemoteConnection connection;
    private List<String> foundDevices = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ProgressBar progressBar;
    private TextView tvScanStatus;
    private ExecutorService scanExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        prefs = new Prefs(this);
        connection = new RemoteConnection(prefs.getTvIp());

        progressBar = findViewById(R.id.progressBar);
        tvScanStatus = findViewById(R.id.tvScanStatus);

        ListView listView = findViewById(R.id.listDevices);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foundDevices);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String ip = foundDevices.get(position).split(" ")[0];
            connectToDevice(ip, "التيليفزيون");
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnScan).setOnClickListener(v -> startScan());
        findViewById(R.id.btnManual).setOnClickListener(v -> showManualDialog());

        // Show current device if any
        String currentIp = prefs.getTvIp();
        if (!currentIp.isEmpty()) {
            tvScanStatus.setText("متصل بـ " + currentIp);
        }
    }

    private void startScan() {
        foundDevices.clear();
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);
        tvScanStatus.setText("جارٍ البحث...");

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int base = dhcp.ipAddress & dhcp.netmask;

        scanExecutor = Executors.newFixedThreadPool(32);
        AtomicInteger remaining = new AtomicInteger(254);

        for (int i = 1; i <= 254; i++) {
            final int host = i;
            scanExecutor.execute(() -> {
                try {
                    int ipInt = base | (host << 24 >>> 24);
                    // Convert little-endian int to IP string
                    String ip = String.format("%d.%d.%d.%d",
                        ipInt & 0xff, (ipInt >> 8) & 0xff,
                        (ipInt >> 16) & 0xff, (ipInt >> 24) & 0xff);
                    InetAddress addr = InetAddress.getByName(ip);
                    if (addr.isReachable(500)) {
                        // Check if TV remote port is open
                        try (java.net.Socket s = new java.net.Socket()) {
                            s.connect(new java.net.InetSocketAddress(ip, RemoteConnection.UDP_PORT), 300);
                            runOnUiThread(() -> {
                                foundDevices.add(ip + " (تليفزيون)");
                                adapter.notifyDataSetChanged();
                            });
                        } catch (Exception ignored) {
                            // Port not open - not a TV remote receiver, but still show reachable hosts
                            runOnUiThread(() -> {
                                foundDevices.add(ip);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    }
                } catch (Exception ignored) {}
                if (remaining.decrementAndGet() == 0) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvScanStatus.setText(foundDevices.isEmpty()
                            ? "لم يُعثر على أجهزة" : "تم العثور على " + foundDevices.size() + " جهاز");
                    });
                }
            });
        }
    }

    private void showManualDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("مثال: 192.168.1.100");
        input.setPadding(40, 20, 40, 20);
        new AlertDialog.Builder(this)
            .setTitle("إدخال IP يدوياً")
            .setView(input)
            .setPositiveButton("اتصال", (d, w) -> {
                String ip = input.getText().toString().trim();
                if (!ip.isEmpty()) connectToDevice(ip, "التيليفزيون");
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void connectToDevice(String ip, String name) {
        prefs.setTvIp(ip);
        prefs.setTvName(name);
        connection.setTvIp(ip);
        Toast.makeText(this, "متصل بـ " + ip, Toast.LENGTH_SHORT).show();
        tvScanStatus.setText("متصل بـ " + ip);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanExecutor != null) scanExecutor.shutdownNow();
        if (connection != null) connection.shutdown();
    }
}
