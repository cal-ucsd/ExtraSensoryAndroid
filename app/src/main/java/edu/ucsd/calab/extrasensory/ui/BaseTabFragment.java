package edu.ucsd.calab.extrasensory.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * Created by Yonatan on 3/3/2015.
 */
public class BaseTabFragment extends Fragment {

    private static final String LOG_TAG = "[ESBaseTabFragment]";

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(LOG_TAG, "Broadcast receiver caught null intent");
                return;
            }
            if (ESDatabaseAccessor.BROADCAST_DATABASE_RECORDS_UPDATED.equals(intent.getAction())) {
                Log.v(LOG_TAG,"Caught database records-updated broadcast");
                reactToRecordsUpdatedEvent();
                return;
            }
            if (ESNetworkAccessor.BROADCAST_NETWORK_QUEUE_SIZE_CHANGED.equals(intent.getAction())) {
                Log.v(LOG_TAG, "Caught network queue broadcast");
                reactToNetworkQueueSizeChangedEvent();
            }

        }
    };

    protected void reactToNetworkQueueSizeChangedEvent() {
        Log.d(LOG_TAG,"reacting to network queue size change");
    }

    protected void reactToRecordsUpdatedEvent() {
        Log.d(LOG_TAG,"reacting to records-update");
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(ESApplication.getTheAppContext());
        localBroadcastManager.registerReceiver(_broadcastReceiver, new IntentFilter(ESDatabaseAccessor.BROADCAST_DATABASE_RECORDS_UPDATED));
        localBroadcastManager.registerReceiver(_broadcastReceiver,new IntentFilter(ESNetworkAccessor.BROADCAST_NETWORK_QUEUE_SIZE_CHANGED));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(ESApplication.getTheAppContext()).unregisterReceiver(_broadcastReceiver);
        super.onPause();
    }

}
