package com.flutteride.codweva;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private WifiManager wifiManager;
    private ListView listView;
    private TextView tvStatus, tvCurrentGuess;
    private Button btnScan, btnStartAttack;

    private ArrayList<String> wifiListNames;
    private ArrayAdapter<String> adapter;
    private List<ScanResult> scanResults;
    private ArrayList<String> customWordlist;

    private String selectedTargetSSID = "";
    private int currentGuessIndex = 0;
    private boolean isAttacking = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);

        tvStatus = new TextView(this);
        tvStatus.setText("الحالة: اضغط بحث للبدء");
        tvStatus.setTextSize(16);
        mainLayout.addView(tvStatus);
        
        tvCurrentGuess = new TextView(this);
        tvCurrentGuess.setText("جاري انتظار الاختيار...");
        tvCurrentGuess.setTextSize(14);
        tvCurrentGuess.setTextColor(Color.BLUE);
        mainLayout.addView(tvCurrentGuess);

        btnScan = new Button(this);
        btnScan.setText("بحث عن الشبكات");
        mainLayout.addView(btnScan);

        listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600);
        listView.setLayoutParams(listParams);
        mainLayout.addView(listView);

        btnStartAttack = new Button(this);
        btnStartAttack.setText("ابدأ التخمينات");
        btnStartAttack.setEnabled(false);
        mainLayout.addView(btnStartAttack);

        setContentView(mainLayout);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiListNames = new ArrayList<>();
        scanResults = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiListNames);
        listView.setAdapter(adapter);

        setupWordlist();

        btnScan.setOnClickListener(v -> checkPermissionsAndScan());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (scanResults != null && position < scanResults.size()) {
                selectedTargetSSID = scanResults.get(position).SSID;
                tvStatus.setText("الشبكة المحددة: " + selectedTargetSSID);
                btnStartAttack.setEnabled(true);
            }
        });

        btnStartAttack.setOnClickListener(v -> {
            if (!selectedTargetSSID.isEmpty() && !isAttacking) {
                startAttackingLoop();
            }
        });
    }

    private void setupWordlist() {
        customWordlist = new ArrayList<>();
        customWordlist.add("10001000"); customWordlist.add("20002000");
        customWordlist.add("30003000"); customWordlist.add("40004000");
        customWordlist.add("50005000"); customWordlist.add("60006000");
        customWordlist.add("70007000"); customWordlist.add("80008000");
        customWordlist.add("90009000"); customWordlist.add("90008000");
        customWordlist.add("80007000"); customWordlist.add("70006000");
        customWordlist.add("60005000"); customWordlist.add("50004000");
        customWordlist.add("40003000"); customWordlist.add("30002000");
        customWordlist.add("20001000"); customWordlist.add("10003000");
        customWordlist.add("20004000"); customWordlist.add("30005000");
        customWordlist.add("40006000"); customWordlist.add("50007000");
        customWordlist.add("60008000"); customWordlist.add("70009000");
        customWordlist.add("90007000"); customWordlist.add("80006000");
        customWordlist.add("70005000"); customWordlist.add("60004000");
        customWordlist.add("50003000"); customWordlist.add("40002000");
        customWordlist.add("30001000"); customWordlist.add("100200300");
    }

    private void checkPermissionsAndScan() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            scanWifiNetworks();
        }
    }

    private void scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success = wifiManager.startScan();
        if (success) {
            Toast.makeText(this, "جاري البحث...", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            scanResults = wifiManager.getScanResults();
            wifiListNames.clear();
            for (ScanResult result : scanResults) {
                if (result.SSID != null && !result.SSID.isEmpty()) {
                    wifiListNames.add(result.SSID);
                }
            }
            adapter.notifyDataSetChanged();
            try {
                unregisterReceiver(this);
            } catch (Exception e) {}
        }
    };

    private void startAttackingLoop() {
        isAttacking = true;
        currentGuessIndex = 0;
        btnStartAttack.setEnabled(false);
        runNextGuess();
    }

    private void runNextGuess() {
        if (currentGuessIndex < customWordlist.size()) {
            String guess = customWordlist.get(currentGuessIndex);
            tvCurrentGuess.setText("تجربة: " + guess);
            tvStatus.setText("الهجوم على: " + selectedTargetSSID + " (" + (currentGuessIndex + 1) + "/" + customWordlist.size() + ")");

            handler.postDelayed(() -> {
                currentGuessIndex++;
                runNextGuess();
            }, 1000);
        } else {
            tvStatus.setText("تم الانتهاء من القائمة.");
            tvCurrentGuess.setText("الحالة: انتهى الفحص");
            btnStartAttack.setEnabled(true);
            isAttacking = false;
        }
    }
}
