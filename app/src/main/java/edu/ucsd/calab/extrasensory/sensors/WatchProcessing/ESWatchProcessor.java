package edu.ucsd.calab.extrasensory.sensors.WatchProcessing;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.app.Activity;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by rafaelaguayo on 6/1/15.
 */
public class ESWatchProcessor extends Activity {

    //static part of class
    private static ESWatchProcessor theSingleWatchProcessor;
    private static final String LOG_TAG = "[ESWatchProcessor]";

    private static final UUID PEBBLE_APP_UUID = UUID.fromString("668eb2d2-73dd-462d-b079-33f0f70ad3d0");
    private static final String RAW_WATCH_ACC_X = "raw_watch_acc_x";
    private static final String RAW_WATCH_ACC_Y = "raw_watch_acc_y";
    private static final String RAW_WATCH_ACC_Z = "raw_watch_acc_z";
    private static final String WATCH_COLLECTION_ON = "TURN ON";
    private static final String WATCH_COLLECTION_OFF = "TURN OFF";
    private static final String YES_ANSWER = "YES";
    private static final String NO_ANSWER = "NO";
    private static final int ACTIVITY_RESPONSE_LENGTH = 1;
    private static final int ACCEL_RESPONSE_LENGTH = 25;
    private static final int WATCH_MESSAGE_KEY = 42;
    private static final int MAX_WATCH_SAMPLES = 500;

    // non-static part
    private HashMap<String, ArrayList<Integer>> _watchAccVals;

    /**
     * Get the single instance of this class
     * @return ESWatchProcessor
     */
    public static ESWatchProcessor getESSensorManager() {
        if (theSingleWatchProcessor == null) {
            theSingleWatchProcessor = new ESWatchProcessor();
        }

        return theSingleWatchProcessor;
    }

    /* Function to let app know if a watch is connected */
    public boolean isWatchConnected() {
        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(LOG_TAG, "Pebble is " + (connected ? "connected" : "not connected"));
        return connected;
    }

    /* Send activity question to watch */
    public void nagUserWithQuestion(String str)
    {
        Log.i(LOG_TAG, "Nagging user with question: " + str);
        PebbleDictionary data = new PebbleDictionary();

        // Add a key of 2, and a string value.
        data.addString(2, str);

        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
    }

    // TODO implement functionality to hold data labels
    public void setUserInfo()
    {

    }

    /*
    Send message to watch to turn on accel collection
    */
    public void startWatchCollection()
    {
        Log.i(LOG_TAG, "Resetting watch bundle data structures.");
        _watchAccVals = new HashMap<>(3);
        _watchAccVals.put(RAW_WATCH_ACC_X, new ArrayList<Integer>(MAX_WATCH_SAMPLES));
        _watchAccVals.put(RAW_WATCH_ACC_Y, new ArrayList<Integer>(MAX_WATCH_SAMPLES));
        _watchAccVals.put(RAW_WATCH_ACC_Z, new ArrayList<Integer>(MAX_WATCH_SAMPLES));

        PebbleDictionary data = new PebbleDictionary();

        // Add a key of 1, and a string value.
        data.addString(1, WATCH_COLLECTION_ON);

        Log.i(LOG_TAG, "Sending message to watch to turn ON accel collection.");
        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
    }

    // Send message to watch to turn off accel collection
    public void stopWatchCollection()
    {
        Log.i(LOG_TAG, "Sending message to watch to turn OFF accel collection.");
        PebbleDictionary data = new PebbleDictionary();

        data.addString(1, WATCH_COLLECTION_OFF);

        PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
    }

    /* Return the watch acceleration data */
    public HashMap<String, ArrayList<Integer>> getWatchAccelData()
    {
        return _watchAccVals;
    }

    /*ONE call to this function to register the receive handler where
     messages received from the watch, will be sent here */
    public void registerReceiveHandler() {

        PebbleKit.registerReceivedDataHandler(this, new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                if(data == null)
                {
                    Log.e(LOG_TAG, "Watch sent null message. Return.");
                    return;
                }
                if(data.size() != ACTIVITY_RESPONSE_LENGTH || data.size() != ACCEL_RESPONSE_LENGTH)
                {
                    Log.e(LOG_TAG, "Message from watch is not reasonable length: " + data.size()
                    + ". With message: " + data.toJsonString());
                }

                if ((data.getString(WATCH_MESSAGE_KEY) != null))
                {
                    // process yes or not now message from response by user
                    if(data.getString(WATCH_MESSAGE_KEY).equals(YES_ANSWER))
                    {
                        // TODO Handle yes reponse from watch
                    }
                    else if(data.getString(WATCH_MESSAGE_KEY).equals(NO_ANSWER))
                    {
                        // TODO Handle not now response from watch
                    }
                    else {
                        Log.e(LOG_TAG, "Message with key sent invalid string response!");
                    }
                }

                if(_watchAccVals == null)
                {
                    Log.e(LOG_TAG, "Watch data bundle is null! startWatchCollection should have been called!");
                    return;
                }

                if(_watchAccVals.get(RAW_WATCH_ACC_X).size() == MAX_WATCH_SAMPLES)
                {
                    Log.i(LOG_TAG, "Have enough watch samples, stopping watch accel collection.");
                    theSingleWatchProcessor.stopWatchCollection();
                    return;
                }

                for(int i = 0; i < data.size(); i++) {
                    String xyzStr = data.getString(i);
                    String [] xyzArr = xyzStr.split(",");
                    _watchAccVals.get(RAW_WATCH_ACC_X).add(Integer.parseInt(xyzArr[0]));
                    _watchAccVals.get(RAW_WATCH_ACC_Y).add(Integer.parseInt(xyzArr[1]));
                    _watchAccVals.get(RAW_WATCH_ACC_Z).add(Integer.parseInt(xyzArr[2]));
                }

                PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);
            }

        });
    }

    /* Function to open the ExtraSensory watch app */
    public void launchWatchApp()
    {
        PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }

    /* Function to close the ExtraSensory watch app */
    public void closeWatchApp()
    {
        PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
    }
}
