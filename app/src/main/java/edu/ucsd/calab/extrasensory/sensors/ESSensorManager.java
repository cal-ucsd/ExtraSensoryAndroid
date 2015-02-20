package edu.ucsd.calab.extrasensory.sensors;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

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
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;

/**
 * This class is to handle the activation of sensors for the recording period,
 * collecting the measured data and bundling it together.
 *
 * This class is designed as a singleton (maximum of 1 instance will be created),
 * in order to avoid collisions and to make sure only a single thread uses the sensors at any time.
 *
 * Created by Yonatan on 1/15/2015.
 */
public class ESSensorManager
        implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    public static final String BROADCAST_RECORDING_STATE_CHANGED = "edu.ucsd.calab.extrasensory.broadcast.recording_state";

    // Static part of the class:
    private static ESSensorManager theSingleSensorManager;
    private static final String LOG_TAG = "[ESSensorManager]";

    private static final int SAMPLE_PERIOD_MICROSECONDS = 25000;
    private static final int NUM_SAMPLES_IN_SESSION = 800;
    private static final double NANOSECONDS_IN_SECOND = 1e9f;
    private static final double MILLISECONDS_IN_SECOND = 1000;
    private static final long LOCATION_UPDATE_INTERVAL_MILLIS = 500;
    private static final long LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS = 50;
    private static final String HIGH_FREQ_DATA_FILENAME = "HF_DUR_DATA.txt";

    // Raw motion sensors:
    private static final String RAW_ACC_X = "raw_acc_x";
    private static final String RAW_ACC_Y = "raw_acc_y";
    private static final String RAW_ACC_Z = "raw_acc_z";
    private static final String RAW_ACC_TIME = "raw_acc_timeref";

    private static final String RAW_MAGNET_X = "raw_magnet_x";
    private static final String RAW_MAGNET_Y = "raw_magnet_y";
    private static final String RAW_MAGNET_Z = "raw_magnet_z";
    private static final String RAW_MAGNET_BIAS_X = "raw_magnet_bias_x";
    private static final String RAW_MAGNET_BIAS_Y = "raw_magnet_bias_y";
    private static final String RAW_MAGNET_BIAS_Z = "raw_magnet_bias_z";
    private static final String RAW_MAGNET_TIME = "raw_magnet_timeref";

    private static final String RAW_GYRO_X = "raw_gyro_x";
    private static final String RAW_GYRO_Y = "raw_gyro_y";
    private static final String RAW_GYRO_Z = "raw_gyro_z";
    private static final String RAW_GYRO_DRIFT_X = "raw_gyro_drift_x";
    private static final String RAW_GYRO_DRIFT_Y = "raw_gyro_drift_y";
    private static final String RAW_GYRO_DRIFT_Z = "raw_gyro_drift_z";
    private static final String RAW_GYRO_TIME = "raw_gyro_timeref";

    // Processed motion sensors (software "sensors"):
    private static final String PROC_ACC_X = "processed_user_acc_x";
    private static final String PROC_ACC_Y = "processed_user_acc_y";
    private static final String PROC_ACC_Z = "processed_user_acc_z";
    private static final String PROC_ACC_TIME = "processed_user_acc_timeref";

    private static final String PROC_GRAV_X = "processed_gravity_x";
    private static final String PROC_GRAV_Y = "processed_gravity_y";
    private static final String PROC_GRAV_Z = "processed_gravity_z";
    private static final String PROC_GRAV_TIME = "processed_gravity_timeref";

    private static final String PROC_MAGNET_X = "processed_magnet_x";
    private static final String PROC_MAGNET_Y = "processed_magnet_y";
    private static final String PROC_MAGNET_Z = "processed_magnet_z";
    private static final String PROC_MAGNET_TIME = "processed_magnet_timeref";

    private static final String PROC_GYRO_X = "processed_gyro_x";
    private static final String PROC_GYRO_Y = "processed_gyro_y";
    private static final String PROC_GYRO_Z = "processed_gyro_z";
    private static final String PROC_GYRO_TIME = "processed_gyro_timeref";

    private static final String PROC_ROTATION_X = "processed_rotation_vector_x";
    private static final String PROC_ROTATION_Y = "processed_rotation_vector_y";
    private static final String PROC_ROTATION_Z = "processed_rotation_vector_z";
    private static final String PROC_ROTATION_COS = "processed_rotation_vector_cosine";
    private static final String PROC_ROTATION_ACCURACY = "processed_rotation_vector_accuracy";

    // Location sensors:
    private static final String LOC_LAT = "location_latitude";
    private static final String LOC_LONG = "location_longitude";
    private static final String LOC_ALT = "location_altitude";
    private static final String LOC_SPEED = "location_speed";
    private static final String LOC_HOR_ACCURACY = "location_horizontal_accuracy";
    private static final String LOC_BEARING = "location_bearing";
    private static final String LOC_TIME = "location_timeref";
    private static final double LOC_ACCURACY_UNAVAILABLE = -1;
    private static final double LOC_ALT_UNAVAILABLE = -1000000;
    private static final double LOC_BEARING_UNAVAILABLE = -1;
    private static final double LOC_SPEED_UNAVAILABLE = -1;

    // Low frequency measurements:
    private static final String TEMPERATURE_DEVICE = "temperature_device";
    private static final String TEMPERATURE_AMBIENT = "temperature_ambient";
    private static final String LIGHT = "light";
    private static final String PRESURE = "presure";
    private static final String PROXIMITY = "";

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
    private GoogleApiClient _googleApiClient;

    private HashMap<String,ArrayList<Double>> _highFreqData;
    private JSONObject _lowFreqData;
    private String _timestampStr;

    private ArrayList<Sensor> _hiFreqSensors;
    private ArrayList<String> _hiFreqSensorFeatureKeys;

    private boolean _recordingRightNow = false;

    private boolean debugSensorSimulationMode() {
        return ESApplication.debugMode();
    }

    public boolean is_recordingRightNow() {
        return _recordingRightNow;
    }

    private void set_recordingRightNow(boolean recordingRightNow) {
        _recordingRightNow = recordingRightNow;
        Intent broadcast = new Intent(BROADCAST_RECORDING_STATE_CHANGED);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(ESApplication.getTheAppContext());
        manager.sendBroadcast(broadcast);
    }

    /**
     * Making the constructor private, in order to make this class a singleton
     */
    private ESSensorManager() {
        _googleApiClient = new GoogleApiClient.Builder(ESApplication.getTheAppContext())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

        _sensorManager = (SensorManager) ESApplication.getTheAppContext().getSystemService(Context.SENSOR_SERVICE);
        // Initialize the sensors:
        _hiFreqSensors = new ArrayList<>(10);
        _hiFreqSensorFeatureKeys = new ArrayList<>(10);

        // Add raw motion sensors:
        if (!tryToAddSensor(Sensor.TYPE_ACCELEROMETER,"raw accelerometer",RAW_ACC_X)) {
            Log.e(LOG_TAG,"There is no accelerometer. Canceling recording.");
            return;
        }
        tryToAddSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,"raw magnetometer",RAW_MAGNET_X);
        tryToAddSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED,"raw gyroscope",RAW_GYRO_X);
        // Add processed motion sensors:
        tryToAddSensor(Sensor.TYPE_GRAVITY,"gravity",PROC_GRAV_X);
        tryToAddSensor(Sensor.TYPE_LINEAR_ACCELERATION,"linear acceleration",PROC_ACC_X);
        tryToAddSensor(Sensor.TYPE_MAGNETIC_FIELD,"calibrated magnetometer",PROC_MAGNET_X);
        tryToAddSensor(Sensor.TYPE_GYROSCOPE,"calibrated gyroscope",PROC_GYRO_X);
        tryToAddSensor(Sensor.TYPE_ROTATION_VECTOR,"rotation vector",PROC_ROTATION_X);

        Log.v(LOG_TAG,"An instance of ESSensorManager was created.");
    }

    private boolean tryToAddSensor(int sensorType,String nameForLog,String featureKey) {
        Sensor sensor = _sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            Log.i(LOG_TAG,"No available sensor: " + nameForLog);
            return false;
        }
        else {
            Log.i(LOG_TAG,"Adding sensor: " + nameForLog);
            _hiFreqSensors.add(sensor);
            _hiFreqSensorFeatureKeys.add(featureKey);
            return true;
        }
    }

    /**
     * Start a recording session from the sensors,
     * and initiate sending the collected measurements to the server.
     *
     * @param timestampStr A string representation of this session's identifying timestamp
     */
    public void startRecordingSensors(String timestampStr) {
        Log.i(LOG_TAG, "Starting recording for timestamp: " + timestampStr);
        clearRecordingSession();
        set_recordingRightNow(true);
        // Set the new timestamp:
        _timestampStr = timestampStr;
        /////////////////////////
        // This is just for debugging. With the simulator (that doesn't produce actual sensor events):
        if (debugSensorSimulationMode()) {
            simulateRecordingSession();
            return;
        }
        /////////////////////////

        // Start recording location:
        int googleServicesResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ESApplication.getTheAppContext());
        if (googleServicesResult == ConnectionResult.SUCCESS) {
            Log.i(LOG_TAG, "We have google play services");
            _googleApiClient.connect();
        }
        else {
            Log.i(LOG_TAG,"We don't have required google play services, so not using location services.");
        }

        // Start recording hi-frequency sensors:
        for (Sensor sensor : _hiFreqSensors) {
            _sensorManager.registerListener(this,sensor,SAMPLE_PERIOD_MICROSECONDS);
        }

        // Get low-frequency measurements:
        collectLowFrequencyMeasurements();
    }

    private void simulateRecordingSession() {
        for (int i = 0; i < NUM_SAMPLES_IN_SESSION; i ++) {
            addHighFrequencyMeasurement(RAW_ACC_X,0);
            addHighFrequencyMeasurement(RAW_ACC_Y,1);
            addHighFrequencyMeasurement(RAW_ACC_Z,2);
            if (addHighFrequencyMeasurement(RAW_ACC_TIME,111)) {
                finishSessionIfReady();
            }
        }
    }

    private void clearRecordingSession() {
        // Clear the high frequency map:
        _highFreqData = new HashMap<>(20);
        _lowFreqData = new JSONObject();
        // Clear temporary data files:
        ESApplication.getTheAppContext().deleteFile(currentZipFilename());
    }

    /**
     * Stop any recording session, if any is active,
     * and clear any data that was collected from the sensors during the session.
     */
    public void stopRecordingSensors() {
        Log.i(LOG_TAG,"Stopping recording.");
        // Stop listening:
        _sensorManager.unregisterListener(this);
        _googleApiClient.disconnect();

        clearRecordingSession();
        set_recordingRightNow(false);
    }


    /**
     * Add another numeric value to a growing vector of measurements from a sensor.
     *
     * @param key The key of the specific measurement type
     * @param measurement The sampled measurement to be added to the vector
     * @return Did this key collect enough samples in this session?
     */
    private boolean addHighFrequencyMeasurement(String key,double measurement) {
        if (_highFreqData == null) {
            Log.e(LOG_TAG,"Can't add measurement. HF data bundle is null");
        }

        // Check if the vector for this key was already initialized:
        if (!_highFreqData.containsKey(key)) {
            _highFreqData.put(key,new ArrayList<Double>(NUM_SAMPLES_IN_SESSION));
        }

        _highFreqData.get(key).add(measurement);

        if (RAW_ACC_X.equals(key) && (_highFreqData.get(key).size() % 100) == 0) {
            logCurrentSampleSize();
        }

        return (_highFreqData.get(key).size() >= NUM_SAMPLES_IN_SESSION);
    }

    private void logCurrentSampleSize() {
        int accSize = 0;
        if (_highFreqData.containsKey(RAW_ACC_X)) {
            accSize = _highFreqData.get(RAW_ACC_X).size();
        }
        int magnetSize = 0;
        if (_highFreqData.containsKey(PROC_MAGNET_X)) {
            magnetSize = _highFreqData.get(PROC_MAGNET_X).size();
        }
        int gyroSize = 0;
        if (_highFreqData.containsKey(PROC_GYRO_X)) {
            gyroSize = _highFreqData.get(PROC_GYRO_X).size();
        }

        Log.i(LOG_TAG,"Collected acc:" + accSize + ",magnet:" + magnetSize + ",gyro:" + gyroSize);
    }

    private void finishSessionIfReady() {
        if (checkIfShouldFinishSession()) {
            finishSession();
        }
    }

    private void finishSession() {
        Log.i(LOG_TAG,"Finishing recording session.");
        //LocationServices.FusedLocationApi.removeLocationUpdates(_googleApiClient,this);
        _googleApiClient.disconnect();

        set_recordingRightNow(false);

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
        String zipFilename = createZipFile(dataStr);
        Log.i(LOG_TAG,"Created zip file: " + zipFilename);

        // Add this zip file to the network queue:
        if (zipFilename != null) {
            ESNetworkAccessor.getESNetworkAccessor().addToNetworkQueue(zipFilename);
        }
    }


    private String createZipFile(String highFreqDataStr) {
        String zipFilename = currentZipFilename();
        try {
            File zipFile = new File(ESApplication.getZipDir(),zipFilename);
            OutputStream os = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            // Add the data files:
            // The high frequency measurements data:
            zos.putNextEntry(new ZipEntry(HIGH_FREQ_DATA_FILENAME));
            zos.write(highFreqDataStr.getBytes());
            zos.closeEntry();

            // Close the zip:
            zos.close();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG,e.getMessage());
            return null;
        }

        return zipFilename;
    }

    private boolean writeFile(String filename,String content) {
        FileOutputStream fos = null;
        try {
            File outFile = new File(ESApplication.getZipDir(),filename);
            fos = new FileOutputStream(outFile);
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
        return _timestampStr + "-" + ESSettings.uuid() + ".zip";
    }

    private boolean checkIfShouldFinishSession() {
        if (_highFreqData == null) {
            Log.e(LOG_TAG,"high frequency data is null");
            return true;
        }

        // Check expected feature keys:
        for (String featureKey : _hiFreqSensorFeatureKeys) {
            if (!_highFreqData.containsKey(featureKey) ||
                    _highFreqData.get(featureKey) == null ||
                    _highFreqData.get(featureKey).size() < NUM_SAMPLES_IN_SESSION) {
                // Then we should wait for this key's sensor to finish sampling
                return false;
            }
        }

        // All the expected sensors sampled the required amount of data
        Log.d(LOG_TAG,"=== checked and should finish.");
        for (String featureKey : _hiFreqSensorFeatureKeys) {
            Log.d("===", featureKey + ": " + _highFreqData.get(featureKey).size());
        }
        return true;
    }


    private void collectLowFrequencyMeasurements() {
        //TODO............
    }

    // Implementing the SensorEventListener interface:
    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean sensorCollectedEnough = false;
        double timestampSeconds =  ((double)event.timestamp) / NANOSECONDS_IN_SECOND;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                addHighFrequencyMeasurement(RAW_ACC_X,event.values[0]);
                addHighFrequencyMeasurement(RAW_ACC_Y,event.values[1]);
                addHighFrequencyMeasurement(RAW_ACC_Z,event.values[2]);
                sensorCollectedEnough = addHighFrequencyMeasurement(RAW_ACC_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                addHighFrequencyMeasurement(RAW_MAGNET_X,event.values[0]);
                addHighFrequencyMeasurement(RAW_MAGNET_Y,event.values[1]);
                addHighFrequencyMeasurement(RAW_MAGNET_Z,event.values[2]);
                addHighFrequencyMeasurement(RAW_MAGNET_BIAS_X,event.values[3]);
                addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Y,event.values[4]);
                addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Z,event.values[5]);
                sensorCollectedEnough = addHighFrequencyMeasurement(RAW_MAGNET_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                addHighFrequencyMeasurement(RAW_GYRO_X,event.values[0]);
                addHighFrequencyMeasurement(RAW_GYRO_Y,event.values[1]);
                addHighFrequencyMeasurement(RAW_GYRO_Z,event.values[2]);
                addHighFrequencyMeasurement(RAW_GYRO_DRIFT_X,event.values[3]);
                addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Y,event.values[4]);
                addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Z,event.values[5]);
                sensorCollectedEnough = addHighFrequencyMeasurement(RAW_GYRO_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_GRAVITY:
                addHighFrequencyMeasurement(PROC_GRAV_X,event.values[0]);
                addHighFrequencyMeasurement(PROC_GRAV_Y,event.values[1]);
                addHighFrequencyMeasurement(PROC_GRAV_Z,event.values[2]);
                sensorCollectedEnough = addHighFrequencyMeasurement(PROC_GRAV_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                addHighFrequencyMeasurement(PROC_ACC_X,event.values[0]);
                addHighFrequencyMeasurement(PROC_ACC_Y,event.values[1]);
                addHighFrequencyMeasurement(PROC_ACC_Z,event.values[2]);
                sensorCollectedEnough = addHighFrequencyMeasurement(PROC_ACC_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                addHighFrequencyMeasurement(PROC_MAGNET_X,event.values[0]);
                addHighFrequencyMeasurement(PROC_MAGNET_Y,event.values[1]);
                addHighFrequencyMeasurement(PROC_MAGNET_Z,event.values[2]);
                sensorCollectedEnough = addHighFrequencyMeasurement(PROC_MAGNET_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_GYROSCOPE:
                addHighFrequencyMeasurement(PROC_GYRO_X,event.values[0]);
                addHighFrequencyMeasurement(PROC_GYRO_Y,event.values[1]);
                addHighFrequencyMeasurement(PROC_GYRO_Z,event.values[2]);
                sensorCollectedEnough = addHighFrequencyMeasurement(PROC_GYRO_TIME,timestampSeconds);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                addHighFrequencyMeasurement(PROC_ROTATION_X,event.values[0]);
                addHighFrequencyMeasurement(PROC_ROTATION_Y,event.values[1]);
                addHighFrequencyMeasurement(PROC_ROTATION_Z,event.values[2]);
//                addHighFrequencyMeasurement(PROC_ROTATION_COS,event.values[4]);
//                addHighFrequencyMeasurement(PROC_ROTATION_ACCURACY,event.values[5]);
                break;
            default:
                Log.e(LOG_TAG,"Got event from unsupported sensor with type " + event.sensor.getType());
        }

        if (sensorCollectedEnough) {
            // Then we've collected enough samples from accelerometer,
            // and we can stop listening to it.
            _sensorManager.unregisterListener(this,event.sensor);
            finishSessionIfReady();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(LOG_TAG,"google api: connected");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL_MILLIS);
        locationRequest.setFastestInterval(LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(_googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG,"google api connection suspended. " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG,"google api connection failed. result: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        double timerefSeconds;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            timerefSeconds = ((double) location.getElapsedRealtimeNanos()) / NANOSECONDS_IN_SECOND;
        }
        else {
            timerefSeconds = ((double) location.getTime()) / MILLISECONDS_IN_SECOND;
        }
        Log.d(LOG_TAG,"got location update with time reference: " + timerefSeconds);

        addHighFrequencyMeasurement(LOC_TIME, timerefSeconds);
        addHighFrequencyMeasurement(LOC_LAT,location.getLatitude());
        addHighFrequencyMeasurement(LOC_LONG,location.getLongitude());

        addHighFrequencyMeasurement(LOC_HOR_ACCURACY,location.hasAccuracy() ? location.getAccuracy() : LOC_ACCURACY_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_ALT,location.hasAltitude() ? location.getAltitude() : LOC_ALT_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_SPEED,location.hasSpeed() ? location.getSpeed() : LOC_SPEED_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_BEARING,location.hasBearing() ? location.getBearing() : LOC_BEARING_UNAVAILABLE);
    }
}
