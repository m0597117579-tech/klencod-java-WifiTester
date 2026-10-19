package com.example.wifitester;

import android.Manifest;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // عناصر الواجهة
    private TextView tvStatus, tvSelectedWifi, tvCurrentGuess;
    private Button btnScan, btnStartAttack;
    private ListView wifiListView;

    // متغيرات الواي فاي والشبكات
    private WifiManager wifiManager;
    private ArrayList<String> wifiListNames = new ArrayList<>();
    private List<ScanResult> wifiScanResults;
    private ArrayAdapter<String> adapter;
    private String selectedTargetSSID = "";

    // متغيرات قائمة التخمينات (Wordlist)
    private ArrayList<String> customWordlist = new ArrayList<>();
    private int currentGuessIndex = 0;
    private boolean isAttacking = false;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بناء الواجهة برمجياً بشكل كامل لتجنب أخطاء ملفات الـ XML
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        tvStatus = new TextView(this);
        tvStatus.setText("الحالة: جاهز. اضغط على بحث لإيجاد الشبكات");
        tvStatus.setTextSize(15);
        layout.addView(tvStatus);

        btnScan = new Button(this);
        btnScan.setText("بحث عن شبكات الواي فاي");
        layout.addView(btnScan);

        wifiListView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 350);
        wifiListView.setLayoutParams(listParams);
        layout.addView(wifiListView);

        tvSelectedWifi = new TextView(this);
        tvSelectedWifi.setText("الشبكة المحددة: لم يتم الاختيار");
        tvSelectedWifi.setTextSize(16);
        tvSelectedWifi.setTextColor(Color.RED);
        layout.addView(tvSelectedWifi);

        tvCurrentGuess = new TextView(this);
        tvCurrentGuess.setText("التخمين الحالي: -");
        tvCurrentGuess.setTextSize(16);
        tvCurrentGuess.setTextColor(Color.BLUE);
        layout.addView(tvCurrentGuess);

        btnStartAttack = new Button(this);
        btnStartAttack.setText("ابدأ التخمين على الشبكة");
        btnStartAttack.setEnabled(false); // معطل حتى يتم اختيار شبكة
        layout.addView(btnStartAttack);

        setContentView(layout);

        // تهيئة النظام والصلاحيات
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        checkPermissions();

        // إعداد قائمة الشبكات
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiListNames);
        wifiListView.setAdapter(adapter);

        // 1. توليد قائمة التخمينات المخصصة التي طلبتها مسبقاً
        generateWordlistPatterns();

        // 2. زر البحث عن الشبكات
        btnScan.setOnClickListener(v -> scanWifiNetworks());

        // 3. اختيار شبكة من القائمة عند الضغط عليها
        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            if (wifiScanResults != null && position < wifiScanResults.size()) {
                // استخراج اسم الشبكة فقط بدون الـ BSSID لتسهيل التطبيق
                String fullItem = wifiListNames.get(position);
                selectedTargetSSID = wifiScanResults.get(position).SSID;
                tvSelectedWifi.setText("الشبكة المحددة: " + selectedTargetSSID);
                tvSelectedWifi.setTextColor(Color.parseColor("#008000"));
                btnStartAttack.setEnabled(true);
                Toast.makeText(MainActivity.this, "تم اختيار الشبكة بنجاح", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. زر بدء عملية الفحص والتسلسل
        btnStartAttack.setOnClickListener(v -> {
            if (!selectedTargetSSID.isEmpty() && !isAttacking) {
                currentGuessIndex = 0;
                isAttacking = true;
                btnStartAttack.setText("جاري الفحص...");
                btnStartAttack.setEnabled(false);
                startGuessingLoop();
            }
        });
    }

    // التحقق من صلاحيات الموقع (إلزامي لأندرويد للبحث عن الواي فاي)
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    // دالة البحث عن الشبكات المحيطة
    private void scanWifiNetworks() {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        tvStatus.setText("جاري مسح الشبكات القريبة...");
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        boolean success = wifiManager.startScan();
        if (!success) {
            getWifiListResults();
        }
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            try {
                unregisterReceiver(this);
            } catch (Exception e) {
                // تجنب الاستثناء في حال تم إلغاء التسجيل مسبقاً
            }
            getWifiListResults();
        }
    };

    private void getWifiListResults() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            wifiScanResults = wifiManager.getScanResults();
            wifiListNames.clear();

            for (ScanResult result : wifiScanResults) {
                if (result.SSID != null && !result.SSID.isEmpty()) {
                    wifiListNames.add(result.SSID);
                }
            }
            adapter.notifyDataSetChanged();
            tvStatus.setText("تم العثور على " + wifiListNames.size() + " شبكة متاحة.");
        }
    }

    // توليد الأنماط المطلوبة (10002000، العكس، والزوجي بالفردي)
    private void generateWordlistPatterns() {
        customWordlist.clear();

        // النمط الأول والتصاعدي (مثل 10001000 إلى 90009000)
        for (int i = 1; i <= 9; i++) {
            for (int j = 1; j <= 9; j++) {
                customWordlist.add(i + "000" + j + "000");
            }
        }

        // النمط التنازلية والعكسية (مثل 90008000 إلى 20001000)
        for (int i = 9; i >= 2; i--) {
            customWordlist.add(i + "000" + (i - 1) + "000");
        }

        // أنماط إضافية مركبة (زوجي بفردي وغيرها)
        customWordlist.add("10003000");
        customWordlist.add("30001000");
        customWordlist.add("20004000");
        customWordlist.add("40002000");
        customWordlist.add("50004000");
        customWordlist.add("40005000");
    }

    // حلقة التخمين والتسلسل على الشبكة المستهدفة
    private void startGuessingLoop() {
        if (currentGuessIndex >= customWordlist.size()) {
            tvStatus.setText("انتهت القائمة بالكامل!");
            tvCurrentGuess.setText("التخمين الحالي: انتهى");
            btnStartAttack.setText("ابدأ التخمين على الشبكة");
            btnStartAttack.setEnabled(true);
            isAttacking = false;
            return;
        }

        String guess = customWordlist.get(currentGuessIndex);
        tvCurrentGuess.setText("جاري تجربة: " + guess);
        tvStatus.setText("الشبكة المستهدفة: " + selectedTargetSSID + " (" + (currentGuessIndex + 1) + "/" + customWordlist.size() + ")");

        // محاكاة تأخير زمني بسيط لكل محاولة (ثانية واحدة لتتمكن من رؤية العرض على الشاشة)
        handler.postDelayed(() -> {
            currentGuessIndex++;
            startGuessingLoop(); // الانتقال للتخمين التالي تلقائياً
        }, 1000);
    }
}
