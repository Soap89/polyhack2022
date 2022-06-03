package com.example.blelocksimulator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {
    private String bleNotSupportedMsg = "Sorry, BLE is not supported on this device, which is required to unlock the facility. Please contact the administrator to request physical access. Apologies for the inconvenience.";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private int numberOfScanRecords = 0;
    private int numberOfSavedRecords = 0;
    private CopyOnWriteArrayList<String> incomingPackets;
    private CopyOnWriteArrayList<Integer> uniqueDeviceIDs;
    private final int MANUFACTURER_ID = 405;
    private TextView scanLogText;
    private GifImageView gifImageView;
    private boolean scanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanLogText = findViewById(R.id.scanLogText);
        gifImageView = findViewById(R.id.gifImageView);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(MainActivity.this, bleNotSupportedMsg, Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) MainActivity.this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, bleNotSupportedMsg, Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            requestBlePermissions(MainActivity.this, 1);
        }

        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    }

    public static void requestBlePermissions(Activity activity, int requestCode) {
        final String[] BLE_PERMISSIONS = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            boolean granted = true;
            for(int i = 0; i < grantResults.length; i++) {
                int res = grantResults[i];
                if(res != PackageManager.PERMISSION_GRANTED)
                    System.out.println("com.example.blelocksimulator: Permission missing: "  + permissions[i]);
                granted &= res == PackageManager.PERMISSION_GRANTED;
            }
            if(granted) {
                System.out.println("com.example.blelocksimulator: All permissions granted.");
                startScanning();
            }

            else {
                System.out.println("com.example.blelocksimulator: Some permission not granted.");
            }
        } else {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Error");
            builder.setMessage("The door cannot be unlocked without access to Bluetooth. Please enable Bluetooth and restart the app.");

            // Set up the buttons
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.cancel();
            });
            builder.show();
        }
        return;
    }

    private void startScanning() {

        HashMap<Integer, String> broadcastingDeviceHashMap = new HashMap<>();

        incomingPackets = new CopyOnWriteArrayList<String>();
        uniqueDeviceIDs = new CopyOnWriteArrayList<>();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;
            System.out.println("No scanning permission!!!!!!");
        }
        bluetoothLeScanner.startScan(null, settings, scanCallback);
        System.out.println("com.example.blelocksimulator: Scanning started successfully.");
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(scanning) {

                numberOfScanRecords = numberOfScanRecords + 1;

                //update UI
                Calendar calendar = Calendar.getInstance();
                Date now = calendar.getTime();

                BLEPacketManager pm = new BLEPacketManager();
                byte[] receivingPacket = result.getScanRecord().getManufacturerSpecificData(MANUFACTURER_ID);

                String receivedMessage = pm.advertisingPacketDecoder(receivingPacket);

                if(receivedMessage != null) {
                    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
                    StringBuilder builder = new StringBuilder("Latest record: " + dateTimeFormat.format(now));
                    builder.append("\n").append("RSSI: " + result.getRssi());
                    builder.append("\n").append("Received message: " + receivedMessage);
                    System.out.println("com.example.blelocksimulator: Received a packet!");

                    int resourceId = getResources().getIdentifier("unlock", "drawable", getPackageName());
                    gifImageView.setImageResource(resourceId);
                    //scanLogText.setText(builder.toString());

                    Toast.makeText(MainActivity.this, "The door has been unlocked successfully. Enjoy!", Toast.LENGTH_SHORT).show();
                    scanning = false;
                    stopScanning();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BLE", "Discovery onScanFailed: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };


    private void stopScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothLeScanner.stopScan(scanCallback);
        scanning = false;
    }
}