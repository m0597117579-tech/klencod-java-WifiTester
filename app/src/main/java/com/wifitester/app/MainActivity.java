package com.flutteride.codweva;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        tvStatus = findViewById(R.id.tvStatus);
        tvCurrentGuess = findViewById(R.id.tvCurrentGuess);
        btnScan = findViewById(R.id.btnScan);
        btnStartAttack = findViewById(R.id.btnStartAttack);

        wifiListNames = new ArrayList<>();
        scanResults = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiListNames);
        listView.setAdapter(adapter);

        // تجهيز كل التخمينات التي طلبتها بالترتيب
        setupWordlist();

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndScan();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (scanResults != null && position < scanResults.size()) {
                    selectedTargetSSID = scanResults.get(position).SSID;
                    tvStatus.setText("تم اختيار الشبكة: " + selectedTargetSSID);
                    btnStartAttack.setEnabled(true);
                }
            }
        });

        btnStartAttack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedTargetSSID.isEmpty()) {
                    Toast.makeText(MainActivity.this, "الرجاء اختيار شبكة أولاً", Toast.LENGTH_SHORT).show();
                    return;
                }
                startAttackingLoop();
            }
        });
    }

    private void setupWordlist() {
        customWordlist = new ArrayList<>();
        
        // إضافـة كافة التخمينات المطلوبة ورا بعضها
        customWordlist.add("10001000");
        customWordlist.add("20002000");
        customWordlist.add("30003000");
        customWordlist.add("40004000");
        customWordlist.add("50005000");
        customWordlist.add("60006000");
        customWordlist.add("70007000");
        customWordlist.add("80008000");
        customWordlist.add("90009000");
        customWordlist.add("90008000");
        customWordlist.add("80007000");
        customWordlist.add("70006000");
        customWordlist.add("60005000");
        customWordlist.add("50004000");
        customWordlist.add("40003000");
        customWordlist.add("30002000");
        customWordlist.add("20001000");
        customWordlist.add("10003000");
        customWordlist.add("20004000");
        customWordlist.add("30005000");
        customWordlist.add("40006000");
        customWordlist.add("50007000");
        customWordlist.add("60008000");
        customWordlist.add("70009000");
        customWordlist.add("90007000");
        customWordlist.add("80006000");
        customWordlist.add("70005000");
        customWordlist.add("60004000");
        customWordlist.add("50003000");
        customWordlist.add("40002000");
        customWordlist.add("30001000");
        customWordlist.add("100200300");
    }

    private void checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            scanWifiNetworks();
        }
    }

    private void scanWifiNetworks() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success = wifiManager.startScan();
        if (success) {
            Toast.makeText(this, "جاري البحث عن الشبكات...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "فشل بدء البحث", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
            } catch (Exception e) {
                // تجنب أي خطأ في إلغاء التسجيل
            }
        }
    };

    private void startAttackingLoop() {
        isAttacking = true;
        currentGuessIndex = 0;
        btnStartAttack.setEnabled(false);
        tvStatus.setText("جاري تجربة التخمينات على: " + selectedTargetSSID);

        runNextGuess();
    }

    private void runNextGuess() {
        if (!isAttacking) return;

        if (currentGuessIndex < customWordlist.size()) {
            String currentGuess = customWordlist.get(currentGuessIndex);
            tvCurrentGuess.setText("التجربة الحالية: " + currentGuess);

            // تأخير زمني ثانية بين كل محاولة وأخرى
            tvCurrentGuess.postDelayed(new Runnable() {
                @Override
                public void run() {
                    currentGuessIndex++;
                    runNextGuess();
                }
            }, 1000);

        } else {
            tvStatus.setText("انتهت قائمة التخمينات بالكامل");
            tvCurrentGuess.setText("التجربة: انتهى");
            btnStartAttack.setEnabled(true);
            isAttacking = false;
        }
    }
}
