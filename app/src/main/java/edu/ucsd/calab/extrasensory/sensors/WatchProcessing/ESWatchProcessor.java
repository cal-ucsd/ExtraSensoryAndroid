package edu.ucsd.calab.extrasensory.sensors.WatchProcessing;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;

/**
 * This class handles the interface of ExtraSensory App's Android-phone component (ESP) with the Pebble-watch component (ESW).
 * This class, and the watch code, were implemented by Rafael Aguayo.
 *
 * Created by rafaelaguayo on 6/1/15.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESWatchProcessor {

    //static part of class
    private static ESWatchProcessor theSingleWatchProcessor;
    private static final String LOG_TAG = "[ESWatchProcessor]";

//    private static final UUID PEBBLE_APP_UUID = UUID.fromString("668eb2d2-73dd-462d-b079-33f0f70ad3d0");
    private static final UUID PEBBLE_APP_UUID = UUID.fromString("7dee2ab7-366e-4f02-aea0-265d66518fb6");
    private static final String RAW_WATCH_ACC_X = "raw_watch_acc_x";
    private static final String RAW_WATCH_ACC_Y = "raw_watch_acc_y";
    private static final String RAW_WATCH_ACC_Z = "raw_watch_acc_z";
    private static final String WATCH_ACC_TIMEREF = "watch_acc_timeref";
    private static final String WATCH_COMPASS_TIMEREF = "watch_compass_timeref";
    private static final String WATCH_COMPASS_HEADING = "watch_compass_heading";
    private static final int WATCH_MESSAGE_TYPE_KEY_RECORDING = 1;
    private static final int WATCH_MESSAGE_TYPE_KEY_ALERT = 2;
    private static final String WATCH_COLLECTION_ON = "TURN ON";
    private static final String WATCH_COLLECTION_OFF = "TURN OFF";
    private static final String YES_ANSWER = "YES";
    private static final String NO_ANSWER = "NO";
    private static final int ACTIVITY_RESPONSE_LENGTH = 1;
    private static final int ACCEL_RESPONSE_LENGTH = 25;
    private static final int ACCEL_SAMPLE_PERIOD_MILLIS = 40;
    private static final int WATCH_MESSAGE_KEY = 42;
    private static final int MAX_WATCH_SAMPLES = 500;
    private static final long WAIT_AFTER_SEND_MESSAGE_IN_MILLIS = 100;

    // non-static part
    private HashMap<String, ArrayList<Integer>> _watchMeasurements;
    private ESApplication _theApplication;

    private Context getTheApplicationContext() {
        return ESApplication.getTheAppContext();
    }
    private ESTimestamp _timestampLatestNotification = null;

    public void setTheESApplicationReference(ESApplication esApplicationReference) {
        _theApplication = esApplicationReference;
    }

    PebbleKit.PebbleDataReceiver _dataReceiver = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

        @Override
        public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
            Log.d(LOG_TAG,"got data: " + data.toJsonString());
            if(data == null)
            {
                Log.e(LOG_TAG, "Watch sent null message. Return.");
                return;
            }
//            if(data.size() != ACTIVITY_RESPONSE_LENGTH && data.size() != ACCEL_RESPONSE_LENGTH)
//            {
//                Log.e(LOG_TAG, "Message from watch (maybe it's compass) is not reasonable length: " + data.size()
//                        + ". With message: " + data.toJsonString());
//            }

            if ((data.size() == 1) && (data.getString(WATCH_MESSAGE_KEY) != null))
            {
                Log.d(LOG_TAG,"Got message of: " + data.getString(WATCH_MESSAGE_KEY));
                // process yes or not now message from response by user
                if(data.getString(WATCH_MESSAGE_KEY).equals(YES_ANSWER))
                {
                    Log.i(LOG_TAG,"User pressed 'yes' on watch.");
                    applySameLabelForRecentActivity();
                    // Since we're handling the user response, we can clear the data for this alert:
                    if (_theApplication != null) {
                        _theApplication.clearDataForAlertForPastFeedback();
                    }
                }
                else if(data.getString(WATCH_MESSAGE_KEY).equals(NO_ANSWER))
                {
                    Log.i(LOG_TAG,"User pressed 'not now' on watch.");
                    // Basically, do nothing. Don't even clear the data from ESApplication or the alert from UI
                }
                else {
                    Log.e(LOG_TAG, "Message with key sent invalid string response: " + data.getString(WATCH_MESSAGE_KEY));
                }

                return;
            }

            if(_watchMeasurements == null)
            {
                Log.e(LOG_TAG, "Watch data bundle is null! startWatchCollection should have been called!");
                return;
            }

            if(_watchMeasurements.get(RAW_WATCH_ACC_X).size() == MAX_WATCH_SAMPLES)
            {
                Log.i(LOG_TAG, "Have enough watch samples, stopping watch accel collection.");
                theSingleWatchProcessor.stopWatchCollection();
                return;
            }

            int timereference = 0;
            int largest_key_expected = data.size();
            for(int key = 0; key < largest_key_expected; key++) {
                String measureStr = data.getString(key);
                if (measureStr == null) {
                    continue;
                }
                String [] xyzArr = measureStr.split(",");
                try {
                    if (xyzArr.length == 3) {
                        // Then this should be an acceleration message:
                        _watchMeasurements.get(RAW_WATCH_ACC_X).add(Integer.parseInt(xyzArr[0]));
                        _watchMeasurements.get(RAW_WATCH_ACC_Y).add(Integer.parseInt(xyzArr[1]));
                        _watchMeasurements.get(RAW_WATCH_ACC_Z).add(Integer.parseInt(xyzArr[2]));
                        _watchMeasurements.get(WATCH_ACC_TIMEREF).add(timereference + ACCEL_SAMPLE_PERIOD_MILLIS*(key-1));
                    }
                    else {
                        String compassParts[] = measureStr.split(":");
                        if (compassParts.length == 2) {
                            // Then this should be a compass heading message:
                            _watchMeasurements.get(WATCH_COMPASS_TIMEREF).add(Integer.parseInt(compassParts[0]));
                            _watchMeasurements.get(WATCH_COMPASS_HEADING).add(Integer.parseInt(compassParts[1]));
                            //Log.d(LOG_TAG,"=== now compass data: " + _watchMeasurements.get(WATCH_COMPASS_HEADING));
                        }
                        else if (key == 0) {
                            // It should be a timestamp for accelerations:
                            timereference = Integer.parseInt(measureStr);
                        }
                        else {
                            Log.e(LOG_TAG,"Got unrecognized message: " + measureStr);
                        }
                    }
                }
                catch(Exception exception) {
                    Log.e(LOG_TAG,"Failed parsing watch measure message: " + measureStr);
                }
            }

            PebbleKit.sendAckToPebble(ESApplication.getTheAppContext(), transactionId);
        }

    };

    BroadcastReceiver _watchConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.INTENT_PEBBLE_CONNECTED:
                    Log.i(LOG_TAG,"Detecting watch connected");
                    launchWatchApp();
                    break;
                case Constants.INTENT_PEBBLE_DISCONNECTED:
                    Log.i(LOG_TAG,"Detecting watch disconnected");
                    LocalBroadcastManager.getInstance(getTheApplicationContext()).unregisterReceiver(_dataReceiver);
                    break;
            }
        }
    };

    private ArrayList<PebbleDictionary> _outgoingMessageQueue;
    //private boolean _currentlySendingMessage = false;
    private long _waitUntilTimeBeforeSendingMessage;

    private ESWatchProcessor() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_PEBBLE_CONNECTED);
        filter.addAction(Constants.INTENT_PEBBLE_DISCONNECTED);
        LocalBroadcastManager.getInstance(getTheApplicationContext()).registerReceiver(_watchConnectionReceiver,filter);
        _outgoingMessageQueue = new ArrayList<>(10);
        _waitUntilTimeBeforeSendingMessage = 0;
    }

    /**
     * Get the single instance of this class
     * @return ESWatchProcessor
     */
    public static ESWatchProcessor getTheWatchProcessor() {
        if (theSingleWatchProcessor == null) {
            theSingleWatchProcessor = new ESWatchProcessor();
        }

        return theSingleWatchProcessor;
    }

    /* Function to let app know if a watch is connected */
    public boolean isWatchConnected() {
        boolean connected = PebbleKit.isWatchConnected(getTheApplicationContext());
        Log.i(LOG_TAG, "Pebble is " + (connected ? "connected" : "not connected"));
        return connected;
    }

    /* Send activity question to watch */
    public void alertUserWithQuestion(String question, ESTimestamp timestampNotification)//,ESApplication.DataForAlertForPastFeedback dataForAlertForPastFeedback)
    {
        _timestampLatestNotification = timestampNotification;
        Log.i(LOG_TAG, "Nagging user with question: " + question);
        PebbleDictionary data = new PebbleDictionary();

        // Add a key of 2, and a string value.
        data.addString(WATCH_MESSAGE_TYPE_KEY_ALERT, question);
        sendMessageToWatch(data);
    }

    private boolean canWeSendWatchMessageNow() {
        long now = new Date().getTime();
        return now > _waitUntilTimeBeforeSendingMessage;
    }

    private void setSendWatiTimeAfterSendingNow() {
        long now = new Date().getTime();
        _waitUntilTimeBeforeSendingMessage = now + WAIT_AFTER_SEND_MESSAGE_IN_MILLIS;
    }

    private void sendMessageToWatch(PebbleDictionary data) {
        // Add the given message to the queue:
        _outgoingMessageQueue.add(data);
        // Go over the messages in the queue until emptied:
        while (!_outgoingMessageQueue.isEmpty()) {
            if (canWeSendWatchMessageNow()) {
                setSendWatiTimeAfterSendingNow();
                if (!_outgoingMessageQueue.isEmpty()) {
                    PebbleDictionary nextMessage = _outgoingMessageQueue.remove(0);
                    PebbleKit.sendDataToPebble(getTheApplicationContext(), PEBBLE_APP_UUID, nextMessage);
                    Log.d(LOG_TAG, ">>> sending message: " + data.toJsonString());
               }
            }
            else {
                try {
                    Log.i(LOG_TAG,"Waiting for sending mechanism to be available...");
                    Thread.sleep(WAIT_AFTER_SEND_MESSAGE_IN_MILLIS);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG,"Failed to sleep thread after sending message to watch.");
                }
            }
        }
    }

    /*
    Send message to watch to turn on accel collection
    */
    public void startWatchCollection()
    {
        Log.i(LOG_TAG, "Resetting watch bundle data structures.");

        PebbleDictionary data = new PebbleDictionary();
        cleanWatchMeasurements();

        // Add a key of 1, and a string value.
        data.addString(WATCH_MESSAGE_TYPE_KEY_RECORDING, WATCH_COLLECTION_ON);

        //launchWatchApp();

        Log.i(LOG_TAG, "Sending message to watch to turn ON accel collection.");
        sendMessageToWatch(data);
    }

    public void cleanWatchMeasurements() {
        _watchMeasurements = new HashMap<>(3);
        _watchMeasurements.put(RAW_WATCH_ACC_X, new ArrayList<Integer>(MAX_WATCH_SAMPLES));
        _watchMeasurements.put(RAW_WATCH_ACC_Y, new ArrayList<Integer>(MAX_WATCH_SAMPLES));
        _watchMeasurements.put(RAW_WATCH_ACC_Z, new ArrayList<Integer>(MAX_WATCH_SAMPLES));
        _watchMeasurements.put(WATCH_ACC_TIMEREF,new ArrayList<Integer>(MAX_WATCH_SAMPLES));

        _watchMeasurements.put(WATCH_COMPASS_TIMEREF,new ArrayList<Integer>(10));
        _watchMeasurements.put(WATCH_COMPASS_HEADING,new ArrayList<Integer>(10));
    }

    // Send message to watch to turn off accel collection
    public void stopWatchCollection()
    {
        Log.i(LOG_TAG, "Sending message to watch to turn OFF accel collection.");
        PebbleDictionary data = new PebbleDictionary();

        data.addString(WATCH_MESSAGE_TYPE_KEY_RECORDING, WATCH_COLLECTION_OFF);
        sendMessageToWatch(data);
    }

    /* Return the watch acceleration data */
    public HashMap<String, ArrayList<Integer>> getWatchMeasurements()
    {
        return _watchMeasurements;
    }

    /*ONE call to this function to register the receive handler where
     messages received from the watch, will be sent here */
    public void registerReceiveHandler() {


    }

    private void applySameLabelForRecentActivity() {
        ESTimestamp timestampUserRespondToWatchNotification = new ESTimestamp();
        if (_theApplication == null || _theApplication.get_dataForAlertForPastFeedback() == null) {
            Log.i(LOG_TAG,"We have no data for alert about past activity.");
            return;
        }
        ESActivity latestVerified = _theApplication.get_dataForAlertForPastFeedback().get_latestVerifiedActivity();
        if (latestVerified == null) {
            Log.i(LOG_TAG,"We have no latest verified activity.");
            return;
        }
        ESContinuousActivity entireRange = ESDatabaseAccessor.getESDatabaseAccessor().getSingleContinuousActivityFromTimeRange(
                latestVerified.get_timestamp(),
                _theApplication.get_dataForAlertForPastFeedback().get_untilTimestamp());
        // Apply the labels of latestVerified to all minutes in the range:
        for (ESActivity minuteActivity : entireRange.getMinuteActivities()) {
            ESDatabaseAccessor.getESDatabaseAccessor().setESActivityValues(
                    minuteActivity,
                    ESActivity.ESLabelSource.ES_LABEL_SOURCE_NOTIFICATION_ANSWER_CORRECT_FROM_WATCH,
                    latestVerified.get_mainActivityUserCorrection(),
                    latestVerified.get_secondaryActivities(),
                    latestVerified.get_moods(),
                    null, null,
                    _timestampLatestNotification,timestampUserRespondToWatchNotification
            );
        }
    }

    /* Function to open the ExtraSensory watch app */
    public void launchWatchApp()
    {
        Log.i(LOG_TAG,"Before registring receive handler, unregister any other receivers...");
        LocalBroadcastManager.getInstance(getTheApplicationContext()).unregisterReceiver(_dataReceiver);
        Log.i(LOG_TAG,"Now register the receive handler...");
        PebbleKit.registerReceivedDataHandler(getTheApplicationContext(),_dataReceiver);
        Log.i(LOG_TAG, "Making sure the watch-side extrasensory app is launched.");
        PebbleKit.startAppOnPebble(getTheApplicationContext(), PEBBLE_APP_UUID);
    }


    /* Function to close the ExtraSensory watch app */
    public void closeWatchApp()
    {
        PebbleKit.closeAppOnPebble(getTheApplicationContext(), PEBBLE_APP_UUID);
    }

}
