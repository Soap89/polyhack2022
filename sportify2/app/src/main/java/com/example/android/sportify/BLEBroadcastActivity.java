package com.example.android.sportify;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;

public class BLEBroadcastActivity extends AppCompatActivity {
    private static final int PERMISSION_CODE = 1001;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private String bleNotSupportedMsg = "Bluetooth Low Energy is not supported on this device.";
    private BluetoothAdapter bluetoothAdapter;
    private int broadcastInterval = 2;
    private int broadcastPower = 3;
    private BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    private final int MANUFACTURER_ID = 405;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_broadcast);
        //requestBlePermissions();
        verifyBLESupport();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            advertise("Hello!");
        } else {
            requestBlePermissions(BLEBroadcastActivity.this, 1);
        }
    }

    // verify BLE support 
    private void verifyBLESupport() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!BLEBroadcastActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(BLEBroadcastActivity.this, bleNotSupportedMsg, Toast.LENGTH_SHORT).show();
            BLEBroadcastActivity.this.finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) BLEBroadcastActivity.this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(BLEBroadcastActivity.this, bleNotSupportedMsg, Toast.LENGTH_SHORT).show();
            BLEBroadcastActivity.this.finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void advertise(String data) {
        //InputMethodManager imm = (InputMethodManager) BLEBroadcastActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(BLEBroadcastActivity.this.getCurrentFocus().getWindowToken(), 0);
        System.out.println("com.example.android.sportify E/ Started advertising");
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(broadcastInterval)
                .setTxPowerLevel(broadcastPower)
                .setConnectable(false)
                .build();

        BLEPacketManager pm = new BLEPacketManager();

        byte[] advertisingData = pm.advertisingPacketBuilder(data);

        AdvertiseData ad = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addManufacturerData(MANUFACTURER_ID, advertisingData)
                .build();

        System.out.println("com.example.android.sportify E/ BLE permission" + ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE));
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {

            return;
        }*/
        advertiser.startAdvertising(settings, ad, advertisingCallback);
    }

    private void stopAdvertise() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("com.example.android.sportify E/ Cannot stop advertising.");
            return;
        }
        advertiser.stopAdvertising(advertisingCallback);
    }

    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e("BLE", "Advertising onStartFailure: " + errorCode);
            super.onStartFailure(errorCode);
        }
    };


    //@Override
    /*protected void onResume() {
        super.onResume();
        startCamera();
    }*/

    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            //Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
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
                    System.out.println("Permission missing: "  + permissions[i]);
                granted &= res == PackageManager.PERMISSION_GRANTED;
            }
            if(granted) {
                System.out.println("com.example.android.sportify E/ All permissions granted.");
                advertise("Hello!");
            }

            else {
                System.out.println("com.example.android.sportify E/ Some permission not granted.");
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


}