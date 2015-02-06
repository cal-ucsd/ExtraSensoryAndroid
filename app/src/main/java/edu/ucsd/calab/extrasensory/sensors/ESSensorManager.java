package edu.ucsd.calab.extrasensory.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.data.ESSettings;

/**
 * This class is to handle the activation of sensors for the recording period,
 * collecting the measured data and bundling it together.
 *
 * This class is designed as a singleton (maximum of 1 instance will be created),
 * in order to avoid collisions and to make sure only a single thread uses the sensors at any time.
 *
 * Created by Yonatan on 1/15/2015.
 */
public class ESSensorManager implements SensorEventListener {

    // Static part of the class:
    private static ESSensorManager theSingleSensorManager;
    private static final String LOG_TAG = "[ESSensorManager]";

    private static final int SAMPLE_PERIOD_MILLIS = 25;
    private static final int NUM_SAMPLES_IN_SESSION = 800;

    private static final float NANOSECONDS_IN_SECOND = 1e-9f;

    private static final String HIGH_FREQ_DATA_FILENAME = "HF_DUR_DATA.txt";

    private static final String ACC_X = "acc_x";
    private static final String ACC_Y = "acc_y";
    private static final String ACC_Z = "acc_z";
    private static final String ACC_TIME = "raw_acc_timeref";

    /**
     * Get the single instance of this class
     * @return
     */
    public static ESSensorManager getESSensorManager() {
        if (theSingleSensorManager == null) {
            theSingleSensorManager = new ESSensorManager();
        }

        return theSingleSensorManager;
    }


    // Non static part:
    private SensorManager _sensorManager;
    private HashMap<String,ArrayList<Float>> _highFreqData;
    private String _timestampStr;

    private Sensor _accelerometer;



    /**
     * Making the constructor private, in order to make this class a singleton
     */
    private ESSensorManager() {
        _sensorManager = (SensorManager) ESApplication.getTheAppContext().getSystemService(Context.SENSOR_SERVICE);
        _accelerometer = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Log.v(LOG_TAG,"An instance of ESSensorManager was created.");
    }

    /**
     * Start a recording session from the sensors,
     * and initiate sending the collected measurements to the server.
     *
     * @param timestampStr A string representation of this session's identifying timestamp
     */
    public void startRecordingSensors(String timestampStr) {
        Log.i(LOG_TAG,"Starting recording.");
        clearRecordingSession();
        // Set the new timestamp:
        _timestampStr = timestampStr;
        if (_accelerometer != null) {
            _sensorManager.registerListener(this,_accelerometer,SAMPLE_PERIOD_MILLIS);
        }
    }

    private void clearRecordingSession() {
        // Clear the high frequency map:
        _highFreqData = new HashMap<>(3);
        // Clear temporary data files:
        //TODO clear the zip and what goes in the zip....
    }

    /**
     * Stop any recording session, if any is active,
     * and clear any data that was collected from the sensors during the session.
     */
    public void stopRecordingSensors() {
        Log.i(LOG_TAG,"Stopping recording.");
        // Stop listening:
        _sensorManager.unregisterListener(this);
        clearRecordingSession();
    }


    /**
     * Add another numeric value to a growing vector of measurements from a sensor.
     *
     * @param key The key of the specific measurement type
     * @param measurement The sampled measurement to be added to the vector
     * @return Did this key collect enough samples in this session?
     */
    private boolean addHighFrequencyMeasurement(String key,float measurement) {
        if (_highFreqData == null) {
            Log.e(LOG_TAG,"Can't add measurement. HF data bundle is null");
        }

        // Check if the vector for this key was already initialized:
        if (!_highFreqData.containsKey(key)) {
            _highFreqData.put(key,new ArrayList<Float>(NUM_SAMPLES_IN_SESSION));
        }

        _highFreqData.get(key).add(measurement);

        return (_highFreqData.get(key).size() >= NUM_SAMPLES_IN_SESSION);
    }

    private void finishSessionIfReady() {
        if (checkIfShouldFinishSession()) {
            finishSession();
        }
    }

    private void finishSession() {
        // Construct an object with all the data:
        JSONObject data = new JSONObject();
        for (String key : _highFreqData.keySet()) {
            JSONArray samples = new JSONArray(_highFreqData.get(key));
            try {
                data.put(key, samples);
            }
            catch (JSONException e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }

        // Save data to file:
        String dataStr = data.toString();
        writeFile(HIGH_FREQ_DATA_FILENAME,dataStr);

        // Zip the files:

        //TODO zip teh data
        //TODO call the network accessor to upload
    }


    private String createZipFile() {
        try {
            OutputStream os = new FileOutputStream(currentZipFilename());
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            // Add the data files:
            ZipEntry zipEntry = new ZipEntry(HIGH_FREQ_DATA_FILENAME);
            /////////////todo
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,e.getMessage());
        }

        return null;
    }

    private boolean writeFile(String filename,String content) {
        FileOutputStream fos = null;
        try {
            fos = ESApplication.getTheAppContext().openFileOutput(filename,Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG,e.getMessage());
            return false;
        }

        return true;
    }

    private String currentZipFilename() {
        return _timestampStr + "-" + ESSettings.uuid();
    }

    private boolean checkIfShouldFinishSession() {
        if (_highFreqData == null) {
            Log.e(LOG_TAG,"high frequency data is null");
            return true;
        }

        // Check accelerometer data:
        if (_accelerometer != null) {
            if (!_highFreqData.containsKey(ACC_X) || _highFreqData.get(ACC_X).size() < NUM_SAMPLES_IN_SESSION) {
                return false;
            }
        }

        return true;
    }

    // Implementing the SensorEventListener interface:
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                addHighFrequencyMeasurement(ACC_X,event.values[0]);
                addHighFrequencyMeasurement(ACC_Y,event.values[1]);
                addHighFrequencyMeasurement(ACC_Z,event.values[2]);
                float timestampSeconds = ((float)event.timestamp) / NANOSECONDS_IN_SECOND;
                if (addHighFrequencyMeasurement(ACC_TIME,timestampSeconds)) {
                    // Then we've collected enough samples from accelerometer,
                    // and we can stop listening to it.
                    _sensorManager.unregisterListener(this,_accelerometer);
                }
                break;
            default:
                Log.e(LOG_TAG,"Got event from unsupported sensor with type " + event.sensor.getType());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
