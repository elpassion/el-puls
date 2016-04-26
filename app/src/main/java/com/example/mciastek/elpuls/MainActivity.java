package com.example.mciastek.elpuls;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zhaoxiaodan.miband.*;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.model.UserInfo;
import com.zhaoxiaodan.miband.model.VibrationMode;

import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EL_PULS";
    private static final String DEVICE_NAME = "MI1S";
    private static final Integer USER_UID = 20111111;

    int heartRateLimit;

    Boolean isRunning = false;
    Boolean isConnected = false;

    UserInfo userInfo = new UserInfo(USER_UID, 1, 25, 176, 77, "Mirek", 0);

    MiBand miband;
    Intent intent;

    final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice device = result.getDevice();

            Log.d(TAG,
                    "Devices: name:" + device.getName() + ",uuid:"
                            + Arrays.toString(device.getUuids()) + ",add:"
                            + device.getAddress() + ",type:"
                            + device.getType() + ",bondState:"
                            + device.getBondState() + ",rssi:" + result.getRssi());


            if (Objects.equals(device.getName(), DEVICE_NAME)) {
                intent.putExtra("device", device);
                miband.connect(device, connectCallback);
                isConnected = true;
            }
        }
    };

    final ActionCallback connectCallback = new ActionCallback() {

        @Override
        public void onSuccess(Object data) {
            MiBand.stopScan(scanCallback);
            attachDisconnectListener();

            miband.setUserInfo(userInfo);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                @Override
                public void run() {
                    attachHeartRateScanner();
                }
            }, 200);

            enableButtonAndHideProgress();
            Log.d(TAG, "connect success");
        }

        @Override
        public void onFail(int errorCode, String msg) {
            Log.d(TAG, "connect fail, code:" + errorCode + ",mgs:" + msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button triggerHeartRateButton = (Button) findViewById(R.id.trigger_heart_rate);

        miband = new MiBand(getApplicationContext());
        intent = new Intent(this, SettingsActivity.class);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        heartRateLimit = sharedPref.getInt(getString(R.string.heart_rate_pref_name), 0);

        Log.d(TAG, "Heart rate limit: " + heartRateLimit);

        if (isConnected) {
            enableButtonAndHideProgress();
        } else {
            MiBand.startScan(scanCallback);
        }

        attachListeners(triggerHeartRateButton);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        heartRateLimit = sharedPref.getInt(getString(R.string.heart_rate_pref_name), 0);

        Log.d(TAG, "onResume - hear rate limit: " + heartRateLimit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Settings");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                if (isConnected) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "You need to connect band first!", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void attachHeartRateScanner() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SystemClock.sleep(200);
                miband.setHeartRateScanListener(new HeartRateNotifyListener() {

                    @Override
                    public void onNotify(int heartRate) {
                        Log.d(TAG, "heart rate: " + heartRate);

                        updateHeartRateText(heartRate);

                        if (heartRateLimit != 0) {
                            if (heartRate > heartRateLimit) {
                                vibrateBand(true);
                                Log.d(TAG, "Heart rate " + heartRate + " is above limit (" + heartRateLimit + ")");
                                return;
                            }
                        }

                        if (isRunning) {
                            miband.startHeartRateScan();
                        }
                    }
                });
            }
        }).start();
    }

    private void updateHeartRateText(final int heartRate) {
        runOnUiThread(new Runnable() {
            TextView resultHeartRateTextView = (TextView) findViewById(R.id.heart_rate_result);

            @Override
            public void run() {
                resultHeartRateTextView.setText("" + heartRate);
            }
        });
    }

    private void vibrateBand(Boolean withLED) {
        final VibrationMode mode;

        if (withLED) {
            mode = VibrationMode.VIBRATION_WITH_LED;
        } else {
            mode = VibrationMode.VIBRATION_WITHOUT_LED;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {


                if (isRunning) {
                    SystemClock.sleep(200);
                    miband.startVibration(mode);

                    SystemClock.sleep(200);
                    miband.startHeartRateScan();
                }
            }
        }).start();
    }

    private void attachListeners(final Button triggerButton) {

        // Trigger button
        triggerButton.setOnClickListener(new View.OnClickListener() {
            Boolean isClicked = false;

            @Override
            public void onClick(View v) {
                if (!isClicked) {
                    miband.startHeartRateScan();
                    Log.d(TAG, "Start hear rate");
                    isRunning = true;
                    triggerButton.setText(R.string.stop_heart_rate);
                } else {
                    isRunning = false;
                    triggerButton.setText(R.string.start_heart_rate);
                }

                isClicked = !isClicked;
            }
        });

    }

    private void enableButtonAndHideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.connection_progress).setVisibility(View.GONE);
                findViewById(R.id.connection_text).setVisibility(View.GONE);
                findViewById(R.id.trigger_heart_rate).setEnabled(true);
                findViewById(R.id.trigger_heart_rate).setClickable(true);
            }
        });
    }

    private void attachDisconnectListener() {
        miband.setDisconnectedListener(new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, "Disconnected!");
            }
        });
    }

    private void pairBand() {
        miband.pair(new ActionCallback() {
            @Override
            public void onSuccess(Object data) {
                Log.d(TAG, "pair success");
            }

            @Override
            public void onFail(int errorCode, String msg) {
                Log.d(TAG, "pair failed");
            }
        });
    }
}
