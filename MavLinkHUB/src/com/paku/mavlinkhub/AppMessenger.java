package com.paku.mavlinkhub;

import com.paku.mavlinkhub.enums.UI_MODE;
import com.paku.mavlinkhub.interfaces.IConnectionFailed;
import com.paku.mavlinkhub.interfaces.ISysLogDataLoggedIn;
import com.paku.mavlinkhub.interfaces.IUiModeChanged;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;

public class AppMessenger {

	private static final String TAG = "AppMessenger";

	public Handler appMsgHandler;
	private AppGlobals globalVars;

	public AppMessenger(Context mContext) {

		globalVars = ((AppGlobals) mContext.getApplicationContext());

		appMsgHandler = new Handler(Looper.getMainLooper()) {
			public void handleMessage(Message msg) {

				switch (msg.what) {
				// Received MLmsg
				case AppGlobals.MSG_MAVLINK_MSG_READY:
					// ItemMavLinkMsg mavlinkMsg = (ItemMavLinkMsg) msg.obj;
					break;
				// all data logged in
				case AppGlobals.MSG_DATA_READY_SYSLOG:
					processSysLogDataLoggedIn();
					break;
				case AppGlobals.MSG_DATA_READY_BYTELOG:
					processByteLogDataLoggedIn();
					break;

				case AppGlobals.MSG_CONNECTOR_DATA_READY:
					break;
				case AppGlobals.MSG_CONNECTOR_CONNECTION_FAILED:
					String msgTxt = new String((byte[]) msg.obj);
					processFailedConnection("Connection Failure with message: " + msgTxt);
					break;
				default:
					super.handleMessage(msg);
				}

			}
		};

		startBroadcastReceiverBluetooth();

	}

	// *****************************************
	// UI objects interfaces
	// *****************************************

	// interfaces

	private ISysLogDataLoggedIn listenerRealTimeMavlinkFragment = null;
	private ISysLogDataLoggedIn listenerSysLogFragment = null;
	private IUiModeChanged listenerOnUIModeChanged = null;
	private IConnectionFailed listenerIConnectionFailed = null;

	// registering for interfaces
	public void registerForIConnectionFailed(Fragment fragment) {
		listenerIConnectionFailed = (IConnectionFailed) fragment;
	}

	public void unregisterIConnectionFailed() {
		listenerSysLogFragment = null;
	}

	public void registerForIUiModeChanged(Fragment fragment) {
		listenerOnUIModeChanged = (IUiModeChanged) fragment;
	}

	public void registerRealTimeMavlinkForIDataLoggedIn(Fragment fragment) {
		listenerRealTimeMavlinkFragment = (ISysLogDataLoggedIn) fragment;
	}

	public void registerSysLogForIDataLoggedIn(Fragment fragment) {
		listenerSysLogFragment = (ISysLogDataLoggedIn) fragment;
	}

	public void unregisterSysLogForIDataLoggedIn() {
		listenerSysLogFragment = null;
	}

	public void unregisterRealTimeMavlinkForIDataLoggedIn() {
		listenerRealTimeMavlinkFragment = null;
	}

	// Messages processing routines
	private void processSysLogDataLoggedIn() {

		if (listenerSysLogFragment != null) {
			listenerSysLogFragment.onSysLogDataLoggedIn();
		}
	}

	private void processByteLogDataLoggedIn() {

		if (listenerRealTimeMavlinkFragment != null) {
			listenerRealTimeMavlinkFragment.onSysLogDataLoggedIn();
		}
	}

	private void processFailedConnection(String errorMsg) {
		listenerIConnectionFailed.onConnectionFailed(errorMsg);
	}

	// *****************************************
	// interface end

	// Bluetooth specific messages handling
	// *****************************************

	private void startBroadcastReceiverBluetooth() {

		final IntentFilter intentFilterBluetooth;
		final BroadcastReceiver broadcastReceiverBluetooth;

		intentFilterBluetooth = new IntentFilter();
		intentFilterBluetooth.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
		intentFilterBluetooth.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		intentFilterBluetooth.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		intentFilterBluetooth.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

		// create and register BT BroadcastReceiver
		broadcastReceiverBluetooth = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				final String action = intent.getAction();

				if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
					final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
							BluetoothAdapter.ERROR);

					switch (state) {
					case BluetoothAdapter.STATE_CONNECTING:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_CONNECTION_STATE_CHANGED]: STATE_CONNECTING");
						break;
					case BluetoothAdapter.STATE_CONNECTED:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_CONNECTION_STATE_CHANGED]: STATE_CONNECTED");
						break;
					case BluetoothAdapter.STATE_DISCONNECTING:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_CONNECTION_STATE_CHANGED]: STATE_DISCONNECTING");
						break;
					case BluetoothAdapter.STATE_DISCONNECTED:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_CONNECTION_STATE_CHANGED]: STATE_DISCONNECTED");
						break;
					}

				}

				if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
					final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
					switch (state) {
					case BluetoothAdapter.STATE_OFF:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_STATE_CHANGED]: STATE_OFF");
						globalVars.uiMode = UI_MODE.UI_MODE_STATE_OFF;
						break;
					case BluetoothAdapter.STATE_TURNING_OFF:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_STATE_CHANGED]: TURNING_OFF");
						globalVars.uiMode = UI_MODE.UI_MODE_TURNING_OFF;
						break;
					case BluetoothAdapter.STATE_ON:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_STATE_CHANGED]: STATE_ON"); // 2nd
						// after
						// turning_on
						globalVars.uiMode = UI_MODE.UI_MODE_STATE_ON;
						break;
					case BluetoothAdapter.STATE_TURNING_ON:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_STATE_CHANGED]: TURNING_ON"); // 1st
						// on
						// bt
						// enable
						globalVars.uiMode = UI_MODE.UI_MODE_TURNING_ON;
						break;
					default:
						globalVars.logger.sysLog(TAG, "[BT_ADAPTER_STATE_CHANGED]: unknown");
						break;
					}

				}

				if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
					BluetoothDevice connDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					globalVars.logger.sysLog(TAG,
							"[BT_DEVICE_CONNECTED] " + connDevice.getName() + " [" + connDevice.getAddress() + "]");
					globalVars.uiMode = UI_MODE.UI_MODE_CONNECTED;
				}

				if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
					BluetoothDevice connDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					globalVars.logger.sysLog(TAG, "[BT_DEVICE_DISCONNECTED] " + connDevice.getName() + " ["
							+ connDevice.getAddress() + "]");

					globalVars.uiMode = UI_MODE.UI_MODE_DISCONNECTED;
				}

				if (listenerOnUIModeChanged != null) listenerOnUIModeChanged.onUiModeChanged();

			}
		};

		// finally register this receiver for intents on BT adapter changes
		globalVars.registerReceiver(broadcastReceiverBluetooth, intentFilterBluetooth);

	}

}