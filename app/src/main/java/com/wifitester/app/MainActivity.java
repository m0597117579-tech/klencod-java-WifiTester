package com.flutteride.codweva;

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

        // إنشاء الواجهة برمجياً بالكامل
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);

        tvStatus = new TextView(this);
        tvStatus.setText("الحالة: اضغط بحث للبدء");
        tvStatus.setTextSize(16);
        mainLayout.addView(tvStatus);
        
