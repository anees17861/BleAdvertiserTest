package com.mrinq.bleadvertisertest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private int advertiseMode;
    private int advertisePower;
    private final byte[] ivBytes = hexToByteArray("07080000000000000000000000000000");
    private final int MANUFACTURER_ID = 89;

    private boolean isStopped = false;
    private Handler handler = new Handler();
    private Object activeAdveriseCallback = null;
    private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.e(TAG,"Bluetooth state:"+BluetoothAdapter.getDefaultAdapter().isEnabled());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RadioGroup modeGroup = findViewById(R.id.radioGroupMode);
        Button stop = findViewById(R.id.button2);
        stop.setOnClickListener(view->{
            if(((Button)view).getText().toString().equals("Pause")) {
                stop.setText("Play");
                handler.removeCallbacksAndMessages(null);
                isStopped = true;
                if (activeAdveriseCallback != null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        AdvertisingSetCallback callback = (AdvertisingSetCallback) activeAdveriseCallback;
                        BluetoothLeAdvertiser bluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
                        if (bluetoothLeAdvertiser != null) {
                            bluetoothLeAdvertiser.stopAdvertisingSet(callback);
                        }
                    } else {
                        AdvertiseCallback callback = (AdvertiseCallback) activeAdveriseCallback;
                        BluetoothLeAdvertiser bluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
                        if (bluetoothLeAdvertiser != null) {
                            bluetoothLeAdvertiser.stopAdvertising(callback);
                        }
                    }
                }
            }else{
                isStopped = false;
                stop.setText("Pause");
            }
        });
        registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        modeGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            RadioButton checkedRadioButton = modeGroup.findViewById(i);
            boolean isChecked = checkedRadioButton.isChecked();
            // If the radiobutton that has changed in check state is now checked...
            if (isChecked)
            {
                switch(i){
                    case R.id.radioButtonModeBalanced:
                        advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
                        break;
                    case R.id.radioButtonModeLowLatency:
                        advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY;
                        break;
                    case R.id.radioButtonModeLowPower:
                        advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
                        break;
                }
            }

        });
        RadioGroup powerGroup = findViewById(R.id.radioGroupPower);
        powerGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            RadioButton checkedRadioButton = powerGroup.findViewById(i);
            boolean isChecked = checkedRadioButton.isChecked();
            // If the radiobutton that has changed in check state is now checked...
            if (isChecked)
            {
                switch(i){
                    case R.id.radioButtonPowerHigh:
                        advertisePower = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;
                        break;
                    case R.id.radioButtonPowerMedium:
                        advertisePower = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
                        break;
                    case R.id.radioButtonPowerLow:
                        advertisePower = AdvertiseSettings.ADVERTISE_TX_POWER_LOW;
                        break;
                    case R.id.radioButtonPowerUltraLow:
                        advertisePower = AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW;
                        break;
                }
            }

        });
    }

    public void onAdvertiseButtonPressed(View view){
        final Button advertiseButton=(Button)view;

        Log.i(TAG,"Advertise mode:"+advertiseMode);
        Log.i(TAG,"Advertise power:"+advertisePower);


        AdvertiseData advertiseData=createTxPacket();
        final BluetoothLeAdvertiser bluetoothLeAdvertiser=BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        if(Build.VERSION.SDK_INT<26) {
            AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(advertiseMode)
                    .setTxPowerLevel(advertisePower)
                    .setConnectable(false)
//                .setTimeout(Integer.parseInt(((EditText)findViewById(R.id.input_duration)).getText().toString()))
                    .build();
            AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    super.onStartFailure(errorCode);
                    Log.e("FROM AdvCallback", "Start Adv Failure: " + String.valueOf(errorCode));
                    Toast.makeText(MainActivity.this, "Adv Failure:" + errorCode, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    super.onStartSuccess(settingsInEffect);
                    advertiseButton.setEnabled(false);
                    Log.i("FROM AdvCallBack", "Start Adv Success");
                    handler.postDelayed(() -> {
                        activeAdveriseCallback = null;
                        advertiseButton.setEnabled(true);
                        if(!isStopped)
                            onAdvertiseButtonPressed(advertiseButton);
                    }, Integer.parseInt(((EditText) findViewById(R.id.input_duration)).getText().toString()) + 50);
                }
            };
            activeAdveriseCallback = advertiseCallback;
            if (bluetoothLeAdvertiser == null) {
                Log.e(TAG, "Advertiser not found, isBluetoothOn " + BluetoothAdapter.getDefaultAdapter().isEnabled());
                Toast.makeText(this, "Advertiser not found, isBluetoothOn " + BluetoothAdapter.getDefaultAdapter().isEnabled(), Toast.LENGTH_SHORT).show();
            } else {
                bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
            }

        }
        else{


            AdvertisingSetParameters.Builder parameters = new AdvertisingSetParameters.Builder()
                    .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                    .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                    .setScannable(true)
                    .setConnectable(false)
                    .setLegacyMode(true);

            switch(advertiseMode) {
                case AdvertiseSettings.ADVERTISE_MODE_LOW_POWER:
                    parameters.setInterval(1600); // 1s
                    break;
                case AdvertiseSettings.ADVERTISE_MODE_BALANCED:
                    parameters.setInterval(400); // 250ms
                    break;
                case AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY:
                    parameters.setInterval(160); // 100ms
                    break;
            }
            switch (advertisePower) {
                case AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW:
                    parameters.setTxPowerLevel(-21);
                    break;
                case AdvertiseSettings.ADVERTISE_TX_POWER_LOW:
                    parameters.setTxPowerLevel(-15);
                    break;
                case AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM:
                    parameters.setTxPowerLevel(-7);
                    break;
                case AdvertiseSettings.ADVERTISE_TX_POWER_HIGH:
                    parameters.setTxPowerLevel(1);
                    break;
            }
            int duration = Integer.parseInt(((EditText)findViewById(R.id.input_duration)).getText().toString())<10?1:Integer.parseInt(((EditText)findViewById(R.id.input_duration)).getText().toString())/10;

            AdvertisingSetCallback advertisingSetCallback = new AdvertisingSetCallback() {
                @Override
                public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                    super.onAdvertisingSetStarted(advertisingSet, txPower, status);
                    Log.i(TAG,"Advertising set started:::" + advertisingSet + ",txPower:"+txPower+",status:"+status);
                    if(advertisingSet!=null) {
                        advertisingSet.enableAdvertising(true, 0, 0);
                        advertiseButton.setEnabled(false);
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Start adv error status:"+status, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                    super.onAdvertisingSetStopped(advertisingSet);
                    Log.i(TAG,"Advertising set stopped");
                    activeAdveriseCallback = null;
                    handler.postDelayed(() -> {
                        advertiseButton.setEnabled(true);
                        if(!isStopped)
                            onAdvertiseButtonPressed(advertiseButton);
                    }, 50);

                }

                @Override
                public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enable, int status) {
                    super.onAdvertisingEnabled(advertisingSet, enable, status);
                    Log.i(TAG,"Advertising set enabled:"+enable + "status:" + status);
                    if(enable){
                        activeAdveriseCallback = this;
                        handler.postDelayed(()-> {
                            bluetoothLeAdvertiser.stopAdvertisingSet(this);
                        },duration*10);
                    }
                }
            };
            if (bluetoothLeAdvertiser == null) {
                Log.e(TAG, "Advertiser not found, isBluetoothOn " + BluetoothAdapter.getDefaultAdapter().isEnabled());
                Toast.makeText(this, "Advertiser not found, isBluetoothOn " + BluetoothAdapter.getDefaultAdapter().isEnabled(), Toast.LENGTH_SHORT).show();
            } else {
                bluetoothLeAdvertiser.startAdvertisingSet(parameters.build(), advertiseData, null, null, null, advertisingSetCallback);
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            //Bluetooth not enabled, request user to turn it on
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothStateBroadcastReceiver);
    }

    private AdvertiseData createTxPacket(){
        int challengeID=20;
        int answer=114;
        byte[] nonEncryptedByteArray = new byte[]{0x41};
        byte[] macIdBytes = hexToByteArray("AABBCCDDEEFF");
        byte[] plainByteArray = new byte[]{(byte) (3 & 0xFF),
                (byte) ((3>>8) & 0xFF),
                (byte) challengeID,
                (byte) 20,
                (byte) ((40)&0xFF),
                (byte) (((40)>>8)&0xFF),
                (byte) 4,
                (byte) -90,
                macIdBytes[0],
                macIdBytes[1],
                macIdBytes[2],
                macIdBytes[3],
                macIdBytes[4],
                macIdBytes[5],
                (byte) (answer & 0xff),
                (byte) ((answer>>8) & 0xff),
        };
        byte[] encryptedByteArray = Arrays.copyOfRange(doEncryption(plainByteArray,hexToByteArray("3c22481b38cf76cb88b3451d70029a92")),0,16);
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        try {
            byteArrayStream.write(nonEncryptedByteArray);
            byteArrayStream.write(encryptedByteArray);
            Log.d(TAG,"TxPacket: " + "EncryptedArray: "+byteArrayToHex(byteArrayStream.toByteArray()));
        } catch (IOException e) {
            Log.e(TAG,"TxPacket: " + e.toString());
        }

        byte[] byteArrayToSend = byteArrayStream.toByteArray();
        return new AdvertiseData.Builder()
                .setIncludeDeviceName( false )
                .addManufacturerData(MANUFACTURER_ID, byteArrayToSend)
                .build();
    }
    private static String byteArrayToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    private static byte[] hexToByteArray(String hex) {
        hex = hex.length()%2 != 0?"0"+hex:hex;

        byte[] b = new byte[hex.length() / 2];

        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
    private byte[] doEncryption(byte[] byteArrayToEncrypt,byte[] keyBytes){

        byte[] cipherData=null;
        try{
            cipherData = AESCipher.encrypt(ivBytes, keyBytes, byteArrayToEncrypt);
        }catch(Exception e){
            Log.e(TAG,"Encryption error: " + e.toString());
        }

        return cipherData;
    }
}
