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
 * This class is the base class for the different UI fragments of the main page
 *
 * Created by Yonatan on 3/3/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
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
                return;
            }
            if (ESNetworkAccessor.BROADCAST_FEEDBACK_QUEUE_SIZE_CHANGED.equals(intent.getAction())) {
                Log.v(LOG_TAG, "Caught feedback queue broadcast");
                reactToFeedbackQueueSizeChangedEvent();
            }

        }
    };

    protected void reactToNetworkQueueSizeChangedEvent() {
        Log.d(LOG_TAG,"reacting to network queue size change");
    }

    protected void reactToFeedbackQueueSizeChangedEvent() {
        Log.d(LOG_TAG,"reacting to feedback queue size change");
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
        localBroadcastManager.registerReceiver(_broadcastReceiver,new IntentFilter(ESNetworkAccessor.BROADCAST_FEEDBACK_QUEUE_SIZE_CHANGED));
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(ESApplication.getTheAppContext()).unregisterReceiver(_broadcastReceiver);
        super.onPause();
    }

}
