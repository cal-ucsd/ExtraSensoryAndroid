package edu.ucsd.calab.extrasensory.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * Created by Yonatan on 2/24/2015.
 */
public class BaseActivity extends ActionBarActivity {

    private static final String LOG_TAG = "[BaseActivity]";

    protected Menu _optionsMenu = null;

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(LOG_TAG, "Broadcast receiver caught null intent");
                return;
            }
            if (ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED.equals(intent.getAction())) {
                Log.v(LOG_TAG, "Caught recording state broadcast");
                checkRecordingStateAndSetRedLight();
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        checkRecordingStateAndSetRedLight();
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver,new IntentFilter(ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_broadcastReceiver);
        super.onPause();
    }

    protected void checkRecordingStateAndSetRedLight() {
        if (_optionsMenu == null) {
            // Then there is no place to hide/show the red light:
            return;
        }

        MenuItem redLight = _optionsMenu.findItem(R.id.action_red_circle);
        if (redLight == null) {
            Log.i(LOG_TAG,"Red Light item is null");
            return;
        }
        if (ESSensorManager.getESSensorManager().is_recordingRightNow()) {
            redLight.setVisible(true);
            Log.i(LOG_TAG, "Recording now - turning on red light");
        }
        else {
            redLight.setVisible(false);
            Log.i(LOG_TAG,"Not recording - turning off red light");
        }
    }

}
