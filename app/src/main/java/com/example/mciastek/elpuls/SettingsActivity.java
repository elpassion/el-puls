package com.example.mciastek.elpuls;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.model.BatteryInfo;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "EL_PULS";

    private SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final MiBand miband = new MiBand(getApplicationContext());
        BluetoothDevice device = getIntent().getParcelableExtra("device");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.pref_name), MODE_PRIVATE);
        prefEditor = sharedPref.edit();

        int hearRateLimit = sharedPref.getInt(getString(R.string.heart_rate_pref_name), 0);

        connectBand(miband, device);
        attachButtonListener();
        updateHearRateLimitField(hearRateLimit);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void connectBand(final MiBand miband, BluetoothDevice device) {
        final TextView batteryInfoTextView = (TextView) findViewById(R.id.battery_level);

        miband.connect(device, new ActionCallback() {
            @Override
            public void onSuccess(Object data) {
                Log.d(TAG, "connect success");
                setBatteryInfo(miband, batteryInfoTextView);
            }

            @Override
            public void onFail(int errorCode, String msg) {
                Log.d(TAG, "connect fail, code:" + errorCode + ",mgs:" + msg);
            }
        });
    }

    private void setBatteryInfo(MiBand miband, final TextView textView) {
        miband.getBatteryInfo(new ActionCallback() {
            @Override
            public void onSuccess(Object data) {
                final BatteryInfo info = (BatteryInfo) data;

                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("" + info.getLevel());
                    }
                });
            }

            @Override
            public void onFail(int errorCode, String msg) {
                Log.d(TAG, msg);
            }
        });
    }

    private void attachButtonListener() {
        Button saveButton = (Button) findViewById(R.id.button_settings_save);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setHeartRateLimit();
                finish();
            }
        });
    }

    private void updateHearRateLimitField(int heartRateLimit) {
        TextView heartRateLimitTextView = (TextView) findViewById(R.id.input_heart_rate_limit);
        heartRateLimitTextView.setText("" + heartRateLimit);
    }

    private void setHeartRateLimit() {
        TextView heartRateLimitTextView = (TextView) findViewById(R.id.input_heart_rate_limit);

        CharSequence heartRateText = heartRateLimitTextView.getText();

        if (heartRateText != null) {
            prefEditor.putInt(getString(R.string.heart_rate_pref_name), Integer.parseInt(heartRateText.toString()));
            prefEditor.commit();
        }
    }
}
