package com.example.argama147.linkingsample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.nttdocomo.android.sdaiflib.DeviceInfo;
import com.nttdocomo.android.sdaiflib.GetDeviceInformation;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LikingSample";

    /**
     * デバイスからサービスアプリへの通知を受け取るBroadcastReceiverのActionフィルター.
     */
    private static final String ACTION_NOTIFY = "com.nttdocomo.android.smartdeviceagent.action.NOTIFICATION";
    private static final String EXTRA_DEVICE_ID = "com.nttdocomo.android.smartdeviceagent.extra.DEVICE_ID";
    private static final String EXTRA_DEVICE_UID = "com.nttdocomo.android.smartdeviceagent.extra.DEVICE_UID";
    private static final String EXTRA_BUTTON_ID = "com.nttdocomo.android.smartdeviceagent.extra.NOTIFICATION_DEVICE_BUTTON_ID";

    /** Linkingデバイスからデータを受信するBroadcastReceiver */
    private DeviceDataReceiver mDeviceDataReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //BroadcastReceiverを登録.
        mDeviceDataReceiver = new DeviceDataReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NOTIFY);
        registerReceiver(mDeviceDataReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //BroadcastReceiverを解除.
        unregisterReceiver(mDeviceDataReceiver);
    }

    /**
     * Linkingデバイスからデータを受信するBroadcastReceiverクラス.
     */
    private class DeviceDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null) {
                if ("com.nttdocomo.android.smartdeviceagent.action.NOTIFICATION"
                        .equals(action)) {
                    final String appName = intent.getStringExtra(
                            "com.nttdocomo.android.smartdeviceagent.extra.APP_NAME");
                    final int buttonId = intent.getIntExtra(EXTRA_BUTTON_ID, -1);
                    final int uid = intent.getIntExtra(EXTRA_DEVICE_UID, -1);
                    final int id = intent.getIntExtra(EXTRA_DEVICE_ID, -1);
                    Log.d(TAG, getPackageName() + " [" + appName + "]"
                            + "buttonId=" + buttonId + " id=" + id + " uid=" + uid);

                    final List<DeviceInfo> deviceInfos
                            = (new GetDeviceInformation(MainActivity.this)).getInformation();
                    for (DeviceInfo info : deviceInfos) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("name=").append(info.getName())
                                .append("modelId=").append(info.getModelId())
                                .append(" uid=").append(info.getUniqueId())
                                .append(" state=").append(info.getState());
                        Log.d(TAG, sb.toString());

                        if (info.getState() == 1 && info.getName().contains("Sizuku Led")) {
                            // デバイスがSizuku LEDのUID)だったら.
                            sendLedData(info.getModelId(), info.getUniqueId());
                        }
                    }
                }
            }
        }
    }

    /**
     *  LED Dataを送信する.
     *
     * @param id ModelID
     * @param uid ユニークID
     * */
    private void  sendLedData(int id, int uid) {
        Log.d(TAG, "sendLedData id=" + id + " uid=" + uid);
        final Intent intent = new Intent(
                this.getPackageName() + ".sda.action.OTHER_NOTIFICATION");
        intent.setComponent(new ComponentName("com.nttdocomo.android.smartdeviceagent"
                , "com.nttdocomo.android.smartdeviceagent.RequestReceiver"));
        final String packageName = getPackageName();
        intent.putExtra(packageName + ".sda.extra.DEVICE_ID", id);
        intent.putExtra(packageName + ".sda.extra.DEVICE_UID", new int[]{ uid });
        intent.putExtra(packageName + ".sda.extra.ILLUMINATION",
                new byte[]{
                        0x20,       // LEDパターンの設定項目ID(固定値)
                        0x21 + 0x1, // 指定したパターンID(0x21～)(+ 0～6)
                        0x30,       // LED色の設定項目ID(固定値)
                        1           // 指定した色のID(現在のSizukuの仕様では赤色固定)
                });
        intent.putExtra(packageName + ".sda.extra.DURATION",
                new byte[]{
                        0x10,      // 通知継続時間の設定項目ID(固定値)
                        0x21 + 0x0 // 通知継続時間の設定値ID(0x21～)( + 0～5)
                });
        sendBroadcast(intent);
    }
}
