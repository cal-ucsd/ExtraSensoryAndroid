package edu.ucsd.calab.extrasensory.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.AudioProcessing.ESAudioProcessor;
import edu.ucsd.calab.extrasensory.sensors.WatchProcessing.ESWatchProcessor;

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

    private static final int LOW_FREQ_SAMPLE_PERIOD_MICROSECONDS = 1000000;
    private static final int SAMPLE_PERIOD_MICROSECONDS = 25000;
    private static final int NUM_SAMPLES_IN_SESSION = 800;
    private static final double NANOSECONDS_IN_SECOND = 1e9f;
    private static final double MILLISECONDS_IN_SECOND = 1000;
    private static final long LOCATION_UPDATE_INTERVAL_MILLIS = 500;
    private static final long LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS = 50;
    private static final float LOCATION_BUBBLE_RADIUS_METERS = 500.0f;
    private static final String HIGH_FREQ_DATA_FILENAME = "HF_DUR_DATA.txt";
    private static final int MAX_TIME_RECORDING_IN_SECONDS = 30;

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
    private static final String PROC_ROTATION_TIME = "processed_rotation_vector_timeref";

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
    private static final double LOC_LAT_HIDDEN = -1000;
    private static final double LOC_LONG_HIDDEN = -1000;

    private static final String LOCATION_QUICK_FEATURES = "location_quick_features";
    private static final String LOCATION_FEATURE_STD_LAT = "std_lat";
    private static final String LOCATION_FEATURE_STD_LONG = "std_long";
    private static final String LOCATION_FEATURE_LAT_CHANGE = "lat_change";
    private static final String LOCATION_FEATURE_LONG_CHANGE = "long_change";
    private static final String LOCATION_FEATURE_LAT_DERIV = "mean_abs_lat_deriv";
    private static final String LOCATION_FEATURE_LONG_DERIV = "mean_abs_long_deriv";

    // Low frequency measurements:
    private static final String LOW_FREQ = "low_frequency";

    private static final String TEMPERATURE_AMBIENT = "temperature_ambient";
    private static final String LIGHT = "light";
    private static final String PRESSURE = "pressure";
    private static final String PROXIMITY = "proximity_cm";
    private static final String HUMIDITY = "relative_humidity";

    private static final String WIFI_STATUS = "wifi_status";
    private static final String APP_STATE = "app_state";
    private static final String ON_THE_PHONE = "on_the_phone";
    private static final String BATTERY_LEVEL = "battery_level";
    private static final String BATTERY_STATE = "battery_state";
    private static final String BATTERY_PLUGGED = "battery_plugged";
    private static final String SCREEN_BRIGHT = "screen_brightness";
    private static final String RINGER_MODE = "ringer_mode";

    private static final String HOUR_OF_DAY = "hour_of_day";
    private static final String MINUTE_IN_HOUR = "minute_in_hour";

    // Values of discrete properties:
    private static final String MISSING_VALUE_STR = "missing";

    private static final String BATTERY_STATUS_CHARGING_STR = "charging";
    private static final String BATTERY_STATUS_DISCHARGING_STR = "discharging";
    private static final String BATTERY_STATUS_FULL_STR = "full";
    private static final String BATTERY_STATUS_NOT_CHARGING_STR = "not_charging";
    private static final String BATTERY_STATUS_UNKNOWN_STR = "unknown";
    private static String getStringValueForBatteryStatus(int batteryStatus) {
        switch (batteryStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return BATTERY_STATUS_CHARGING_STR;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return BATTERY_STATUS_DISCHARGING_STR;
            case BatteryManager.BATTERY_STATUS_FULL:
                return BATTERY_STATUS_FULL_STR;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return BATTERY_STATUS_NOT_CHARGING_STR;
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                return BATTERY_STATUS_UNKNOWN_STR;
            default:
                return MISSING_VALUE_STR;
        }
    }

    private static final String	BATTERY_PLUGGED_AC_STR = "ac";
    private static final String	BATTERY_PLUGGED_USB_STR = "usb";
    private static final String	BATTERY_PLUGGED_WIRELESS_STR = "wireless";
    private static String getStringValueForBatteryPlugged(int batteryPlugged) {
        switch (batteryPlugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return BATTERY_PLUGGED_AC_STR;
            case BatteryManager.BATTERY_PLUGGED_USB:
                return BATTERY_PLUGGED_USB_STR;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return BATTERY_PLUGGED_WIRELESS_STR;
            default:
                return MISSING_VALUE_STR;
        }
    }

    private static final String RINGER_MODE_NORMAL_STR = "normal";
    private static final String RINGER_MODE_SILENT_STR = "silent_no_vibrate";
    private static final String RINGER_MODE_VIBRATE_STR = "silent_with_vibrate";
    private static String getStringValueForRingerMode(int ringerMode) {
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                return RINGER_MODE_NORMAL_STR;
            case AudioManager.RINGER_MODE_SILENT:
                return RINGER_MODE_SILENT_STR;
            case AudioManager.RINGER_MODE_VIBRATE:
                return RINGER_MODE_VIBRATE_STR;
            default:
                return MISSING_VALUE_STR;
        }
    }

    private static String getSensorLeadingMeasurementKey(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return RAW_ACC_X;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return RAW_MAGNET_X;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return RAW_GYRO_X;
            case Sensor.TYPE_GRAVITY:
                return PROC_GRAV_X;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return PROC_ACC_X;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return PROC_MAGNET_X;
            case Sensor.TYPE_GYROSCOPE:
                return PROC_GYRO_X;
            case Sensor.TYPE_ROTATION_VECTOR:
                return PROC_ROTATION_X;
            default:
                throw new UnknownError("Requested measurement key for unknown sensor, with type: " + sensorType + " with name: " + getESSensorManager().getSensorNiceName(sensorType));
        }
    }

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
    private ESAudioProcessor _audioProcessor;
    private ESWatchProcessor _watchProcessor;

    private HashMap<String,ArrayList<Double>> _highFreqData;
    private HashMap<String,ArrayList<Double>> _locationCoordinatesData;
    private JSONObject _lowFreqData;
    private ESTimestamp _timestamp;
    private ArrayList<Sensor> _hiFreqSensors;
    private ArrayList<String> _hiFreqSensorFeatureKeys;
    private ArrayList<String> _sensorKeysThatShouldGetEnoughSamples;
    private ArrayList<Sensor> _lowFreqSensors;
    private ArrayList<String> _lowFreqSensorFeatureKeys;
    private Map<Integer,String> _sensorTypeToNiceName;

    private boolean _recordingRightNow = false;

    private boolean debugSensorSimulationMode() {
        return ESApplication.debugMode();
    }

    public boolean is_recordingRightNow() {
        return _recordingRightNow;
    }

    public String getSensorNiceName(int sensorType) {
        Integer type = new Integer(sensorType);
        if (_sensorTypeToNiceName.containsKey(type)) {
            return _sensorTypeToNiceName.get(type);
        }
        else {
            return ""+sensorType;
        }
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
        _lowFreqSensors = new ArrayList<>(10);
        _lowFreqSensorFeatureKeys = new ArrayList<>(10);
        _timestamp = new ESTimestamp(0);
        _sensorTypeToNiceName = new HashMap<>(10);

        // Audio processor:
        _audioProcessor = new ESAudioProcessor();

        // Watch processor:
        _watchProcessor = ESWatchProcessor.getTheWatchProcessor();

        // Add raw motion sensors:
        if (!tryToAddSensor(Sensor.TYPE_ACCELEROMETER,true,"raw accelerometer",RAW_ACC_X)) {
            Log.e(LOG_TAG,"There is no accelerometer. Canceling recording.");
            return;
        }
        tryToAddSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,true,"raw magnetometer",RAW_MAGNET_X);
        tryToAddSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED,true,"raw gyroscope",RAW_GYRO_X);
        // Add processed motion sensors:
        tryToAddSensor(Sensor.TYPE_GRAVITY,true,"gravity",PROC_GRAV_X);
        tryToAddSensor(Sensor.TYPE_LINEAR_ACCELERATION,true,"linear acceleration",PROC_ACC_X);
        tryToAddSensor(Sensor.TYPE_MAGNETIC_FIELD,true,"calibrated magnetometer",PROC_MAGNET_X);
        tryToAddSensor(Sensor.TYPE_GYROSCOPE,true,"calibrated gyroscope",PROC_GYRO_X);
        tryToAddSensor(Sensor.TYPE_ROTATION_VECTOR,true,"rotation vector",PROC_ROTATION_X);

        // Add low frequency sensors:
        tryToAddSensor(Sensor.TYPE_AMBIENT_TEMPERATURE,false,"ambient temperature",TEMPERATURE_AMBIENT);
        tryToAddSensor(Sensor.TYPE_LIGHT,false,"light",LIGHT);
        tryToAddSensor(Sensor.TYPE_PRESSURE,false,"pressure",PRESSURE);
        tryToAddSensor(Sensor.TYPE_PROXIMITY,false,"proximity",PROXIMITY);
        tryToAddSensor(Sensor.TYPE_RELATIVE_HUMIDITY,false,"relative humidity",HUMIDITY);

        // This list can be prepared at every recording session, according to the sensors that should be recorded

        Log.v(LOG_TAG, "An instance of ESSensorManager was created.");
    }

    private boolean tryToAddSensor(int sensorType,boolean isHighFreqSensor, String niceName,String featureKey) {
        Sensor sensor = _sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            Log.i(LOG_TAG,"No available sensor: " + niceName);
            return false;
        }
        else {
            _sensorTypeToNiceName.put(new Integer(sensorType),niceName);
            if (isHighFreqSensor) {
                Log.i(LOG_TAG, "Adding hi-freq sensor: " + niceName);
                _hiFreqSensors.add(sensor);
                _hiFreqSensorFeatureKeys.add(featureKey);
            }
            else {
                Log.i(LOG_TAG, "Adding low-freq sensor: " + niceName);
                _lowFreqSensors.add(sensor);
                _lowFreqSensorFeatureKeys.add(featureKey);
            }
            return true;
        }
    }

    private ArrayList<Integer> getSensorTypesFromSensors(ArrayList<Sensor> sensors) {
        if (sensors == null) {
            return new ArrayList<Integer>(10);
        }
        ArrayList<Integer> sensorTypes = new ArrayList<>(sensors.size());
        for (Sensor sensor : sensors) {
            sensorTypes.add(new Integer(sensor.getType()));
        }
        return sensorTypes;
    }

    public ArrayList<Integer> getRegisteredHighFreqSensorTypes() {
        return getSensorTypesFromSensors(_hiFreqSensors);
    }

    public ArrayList<Integer> getRegisteredLowFreqSensorTypes() {
        return getSensorTypesFromSensors(_lowFreqSensors);
    }

    /**
     * Start a recording session from the sensors,
     * and initiate sending the collected measurements to the server.
     *
     * @param timestamp This recording session's identifying timestamp
     */
    public void startRecordingSensors(ESTimestamp timestamp) {
        Log.i(LOG_TAG, "Starting recording for timestamp: " + timestamp.toString());
        clearRecordingSession(true);
        set_recordingRightNow(true);
        // Set the new timestamp:
        _timestamp = timestamp;
        /////////////////////////
        // This is just for debugging. With the simulator (that doesn't produce actual sensor events):
        if (debugSensorSimulationMode()) {
            simulateRecordingSession();
            return;
        }
        /////////////////////////

        // Start recording location:
        if (ESSettings.shouldRecordLocation()) {
            int googleServicesResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ESApplication.getTheAppContext());
            if (googleServicesResult == ConnectionResult.SUCCESS) {
                Log.i(LOG_TAG, "We have google play services");
                _googleApiClient.connect();
            } else {
                Log.i(LOG_TAG, "We don't have required google play services, so not using location services.");
            }
        }
        else {
            Log.d(LOG_TAG,"As requested: not recording location.");
        }

        // Start recording audio:
        if (ESSettings.shouldRecordAudio()) {
            try {
                _audioProcessor.startRecordingSession();
            } catch (Exception exception) {
                Log.e(LOG_TAG, "Failed to start audio recording session: " + exception.getMessage());
            }
        }
        else {
            Log.d(LOG_TAG,"As requested: not recording audio.");
        }

        // Start recording watch:
        if (ESSettings.shouldRecordWatch()) {
            if (_watchProcessor.isWatchConnected()) {
                _watchProcessor.startWatchCollection();
            }
        }
        else {
            Log.d(LOG_TAG,"As requested: not recording from the watch.");
        }

        // Start recording hi-frequency sensors:
        ArrayList<Integer> hfSensorTypesToRecord = ESSettings.highFreqSensorTypesToRecord();
        prepareListOfMeasurementsShouldGetEnoughSamples(hfSensorTypesToRecord);
        for (Sensor sensor : _hiFreqSensors) {
            if (hfSensorTypesToRecord.contains(new Integer(sensor.getType()))) {
                _sensorManager.registerListener(this, sensor, SAMPLE_PERIOD_MICROSECONDS);
                Log.d(LOG_TAG,"== Registring for recording HF sensor: " + getSensorNiceName(sensor.getType()));
            }
            else {
                Log.d(LOG_TAG,"As requested: not recording HF sensor: " + getSensorNiceName(sensor.getType()));
            }
        }

        // Start low-frequency sensors:
        ArrayList<Integer> lfSensorTypesToRecord = ESSettings.lowFreqSensorTypesToRecord();
        for (Sensor sensor : _lowFreqSensors) {
            if (lfSensorTypesToRecord.contains(new Integer(sensor.getType()))) {
                _sensorManager.registerListener(this, sensor, LOW_FREQ_SAMPLE_PERIOD_MICROSECONDS);
            }
            else {
                Log.d(LOG_TAG,"As requested: not recording LF sensor: " + getSensorNiceName(sensor.getType()));
            }
        }

        // Get phone-state measurements:
        collectPhoneStateMeasurements();

        // Maybe the session is already done:
        finishSessionIfReady();
    }

    private void prepareListOfMeasurementsShouldGetEnoughSamples(ArrayList<Integer> hfSensorTypesToRecord) {
        _sensorKeysThatShouldGetEnoughSamples = new ArrayList<>(10);
        // In case there are no high frequency sensors to record,
        // we shouldn't wait for any measurement-key to fill up with enough samples.
        if (hfSensorTypesToRecord.size() <= 0) {
            return;
        }

        // Otherwise, let's use the policy of having a single leading sensor (or several) that will determine when to stop the recording
        // (whenever that sensor contributed enough samples for its leading measurement key).
        // It is possible that accelerometer will reach the desired number of samples (e.g. 800) while gyroscope
        // will only reach 600 samples. This is because the sensor-sampling systems of Android are not aligned
        // among the sensors, and the sampling rates are not completely stable.
        Integer[] leadingSensorPriority = new Integer[]{
                Sensor.TYPE_ACCELEROMETER,Sensor.TYPE_LINEAR_ACCELERATION,Sensor.TYPE_GRAVITY,
                Sensor.TYPE_MAGNETIC_FIELD,Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
        };
        // Avoiding wating for sensors that tend to be slow samplers, like gyroscope.
        for (Integer sensorTypeInteger : leadingSensorPriority) {
            if (hfSensorTypesToRecord.contains(sensorTypeInteger)) {
                // Then mark this single sensor as the one to wait for to get enough samples:
                Log.d(LOG_TAG,"Marking the leading sensor (the one from which we'll wait to get enough measurements): " + getSensorNiceName(sensorTypeInteger));
                _sensorKeysThatShouldGetEnoughSamples.add(getSensorLeadingMeasurementKey(sensorTypeInteger));
                return;
            }
        }
        // If we reached here, we have a risk:
        // all the high frequency sensors to be recorded are those that we do not trust to sample quickly enough to reach
        // the full number of samples. This can result in a situation where it will take more than a minute
        // before the sensor gets enough samples, and then the new recording session will begin.
        // To avoid this problem, we add an additional max-time-based mechanism to determain when to stop recording.
        Log.w(LOG_TAG,"!!! We have no sensor to tell us when to stop recording.");
    }

    private void simulateRecordingSession() {
        for (int i = 0; i < NUM_SAMPLES_IN_SESSION; i ++) {
            addHighFrequencyMeasurement(RAW_MAGNET_X,0);
            addHighFrequencyMeasurement(RAW_MAGNET_Y,0);
            addHighFrequencyMeasurement(RAW_MAGNET_Z,0);
            addHighFrequencyMeasurement(RAW_MAGNET_BIAS_X, 0);
            addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Y, 0);
            addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Z, 0);

            addHighFrequencyMeasurement(RAW_GYRO_X, 0);
            addHighFrequencyMeasurement(RAW_GYRO_Y, 0);
            addHighFrequencyMeasurement(RAW_GYRO_Z, 0);
            addHighFrequencyMeasurement(RAW_GYRO_DRIFT_X, 0);
            addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Y, 0);
            addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Z, 0);

            addHighFrequencyMeasurement(PROC_GRAV_X, 0);
            addHighFrequencyMeasurement(PROC_GRAV_Y, 0);
            addHighFrequencyMeasurement(PROC_GRAV_Z, 0);

            addHighFrequencyMeasurement(PROC_ACC_X, 0);
            addHighFrequencyMeasurement(PROC_ACC_Y, 0);
            addHighFrequencyMeasurement(PROC_ACC_Z, 0);

            addHighFrequencyMeasurement(PROC_MAGNET_X, 0);
            addHighFrequencyMeasurement(PROC_MAGNET_Y, 0);
            addHighFrequencyMeasurement(PROC_MAGNET_Z, 0);

            addHighFrequencyMeasurement(PROC_GYRO_X, 0);
            addHighFrequencyMeasurement(PROC_GYRO_Y, 0);
            addHighFrequencyMeasurement(PROC_GYRO_Z, 0);

            addHighFrequencyMeasurement(PROC_ROTATION_X, 0);
            addHighFrequencyMeasurement(PROC_ROTATION_Y, 0);
            addHighFrequencyMeasurement(PROC_ROTATION_Z, 0);

            addHighFrequencyMeasurement(RAW_ACC_X,0);
            addHighFrequencyMeasurement(RAW_ACC_Y,1);
            addHighFrequencyMeasurement(RAW_ACC_Z,2);
            if (addHighFrequencyMeasurement(RAW_ACC_TIME,111)) {
                finishSessionIfReady();
            }
        }
    }

    private void clearRecordingSession(boolean clearBeforeStart) {
        // Clear the high frequency map:
        _highFreqData = new HashMap<>(20);

        _locationCoordinatesData = new HashMap<>(2);
        _locationCoordinatesData.put(LOC_LAT,new ArrayList<Double>(10));
        _locationCoordinatesData.put(LOC_LONG,new ArrayList<Double>(10));

        _lowFreqData = new JSONObject();
        // Clear temporary data files:
        ESApplication.getTheAppContext().deleteFile(currentZipFilename());

        // Clear audio data:
        _audioProcessor.clearAudioData();

        // Clear watch data:
        if (!clearBeforeStart && _watchProcessor.isWatchConnected()) {
            _watchProcessor.stopWatchCollection();
        }
        _watchProcessor.cleanWatchMeasurements();
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

        clearRecordingSession(false);
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

        //if (RAW_ACC_X.equals(key) && (_highFreqData.get(key).size() % 100) == 0) {
        if ((_highFreqData.get(key).size() % 100) == 0) {
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

    private void finishIfTooMuchTimeRecording() {
        ESTimestamp now = new ESTimestamp();
        int timeRecording = now.differenceInSeconds(_timestamp);
        if (timeRecording >= MAX_TIME_RECORDING_IN_SECONDS) {
            Log.d(LOG_TAG,"Finishing this recording because it is already too long, num seconds: " + timeRecording);
            finishSession();
        }
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

        // Finish audio recording:
        try {
            _audioProcessor.stopRecordingSession(true);
        }
        catch (Exception exception) {
            Log.e(LOG_TAG,"Failed to stop audio recording session: " + exception.getMessage());
        }

        // Finish watch recording:
        Map<String,ArrayList<Integer>> watchMeasurements = null;
        if (_watchProcessor.isWatchConnected()) {
            _watchProcessor.stopWatchCollection();
            watchMeasurements = _watchProcessor.getWatchMeasurements();
        }

        // Finish any leftover phone sensors:
        _sensorManager.unregisterListener(this);

        set_recordingRightNow(false);

        // Construct an object with all the data:
        JSONObject data = new JSONObject();
        // Add high-frequency data:
        for (String key : _highFreqData.keySet()) {
            JSONArray samples = new JSONArray(_highFreqData.get(key));
            try {
                data.put(key, samples);
            }
            catch (JSONException e) {
                Log.e(LOG_TAG,"JSON: failed putting key " + key + ". Message: " + e.getMessage());
            }
        }

        // Add watch data:
        if (watchMeasurements != null) {
            for (String key : watchMeasurements.keySet()) {
                JSONArray samples = new JSONArray(watchMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting watch key " + key + ". Message: " + e.getMessage());
                }
            }
        }

        // Add low-frequency data:
        try {
            data.put(LOW_FREQ,_lowFreqData);
        } catch (JSONException e) {
            Log.e(LOG_TAG,"JSON: failed putting low frequency data. Message: " + e.getMessage());
        }

        // Add location quick features:
        JSONObject locationQuickFeatures = calcLocationQuickFeatures();
        try {
            data.put(LOCATION_QUICK_FEATURES,locationQuickFeatures);
        } catch (JSONException e) {
            Log.e(LOG_TAG,"JSON: failed putting location quick features. Message: " + e.getMessage());
        }

        // Save data to file:
        String dataStr = data.toString();
        writeFile(HIGH_FREQ_DATA_FILENAME,dataStr);

        // Zip the files:
        String zipFilename = createZipFile(dataStr);
        Log.i(LOG_TAG,"Created zip file: " + zipFilename);

        // Add this zip file to the network queue:
        if (zipFilename != null) {
            ESNetworkAccessor.getESNetworkAccessor().addToUploadQueue(zipFilename);
        }
    }

    private JSONObject calcLocationQuickFeatures() {
        ArrayList<Double> latVals = _locationCoordinatesData.get(LOC_LAT);
        ArrayList<Double> longVals = _locationCoordinatesData.get(LOC_LONG);
        ArrayList<Double> timerefs = _highFreqData.get(LOC_TIME);

        int n = latVals.size();
        if (longVals == null || latVals == null) {
            Log.e(LOG_TAG,"Missing the internally-saved location coordinates.");
            return null;
        }
        if (timerefs == null) {
            Log.e(LOG_TAG,"Missing the timereference of location updates.");
            return null;
        }

        if (longVals.size() != n || timerefs.size() != n) {
            Log.e(LOG_TAG,"Number of longitude values or location timerefs doesn't match number of latitude values");
            return null;
        }
        if (n == 0) {
            return null;
        }

        double sumLat=0,sumLong=0,sumSqLat=0,sumSqLong=0,sumAbsLatDeriv=0,sumAbsLongDeriv=0;
        for (int i=0; i < n; i++) {
            sumLat += latVals.get(i);
            sumLong += longVals.get(i);
            sumSqLat += Math.pow(latVals.get(i),2);
            sumSqLong += Math.pow(longVals.get(i),2);
            if (i>0) {
                double timeDiff = timerefs.get(i)-timerefs.get(i-1);
                if (timeDiff > 0) {
                    sumAbsLatDeriv += Math.abs(latVals.get(i) - latVals.get(i - 1)) / timeDiff;
                    sumAbsLongDeriv += Math.abs(longVals.get(i) - longVals.get(i - 1)) / timeDiff;
                }
                else {
                    sumAbsLatDeriv = -1;
                    sumAbsLongDeriv = -1;
                    break;
                }
            }
        }

        double meanLat = sumLat / n;
        double meanLong = sumLong / n;
        double meanSqLat = sumSqLat / n;
        double meanSqLong = sumSqLong / n;
        double varLat = meanSqLat - Math.pow(meanLat,2);
        double varLong = meanSqLong - Math.pow(meanLong,2);

        double meanAbsLatDeriv = (sumAbsLatDeriv < 0) ? -1 : (n > 1 ? sumAbsLatDeriv / (n-1) : 0);
        double meanAbsLongDeriv = (sumAbsLongDeriv < 0) ? -1 : (n > 1 ? sumAbsLongDeriv / (n-1) : 0);

        double latStd = Math.sqrt(varLat);
        double longStd = Math.sqrt(varLong);
        double latChange = latVals.get(n-1)-latVals.get(0);
        double longChange = longVals.get(n-1)-longVals.get(0);

        Log.d(LOG_TAG,String.format("Calculated location quick features: latChange %f. longChange %f. latStd %f. longStd %f. latDeriv %f. longDerig %f",
                latChange,longChange,latStd,longStd,meanAbsLatDeriv,meanAbsLongDeriv));

        JSONObject locationQuickFeatures = new JSONObject();
        try {
            locationQuickFeatures.put(LOCATION_FEATURE_LAT_CHANGE,latChange);
            locationQuickFeatures.put(LOCATION_FEATURE_LONG_CHANGE,longChange);
            locationQuickFeatures.put(LOCATION_FEATURE_STD_LAT,latStd);
            locationQuickFeatures.put(LOCATION_FEATURE_STD_LONG,longStd);
            locationQuickFeatures.put(LOCATION_FEATURE_LAT_DERIV,meanAbsLatDeriv);
            locationQuickFeatures.put(LOCATION_FEATURE_LONG_DERIV,meanAbsLongDeriv);

            return locationQuickFeatures;
        } catch (JSONException e) {
            Log.e(LOG_TAG,"JSON: failed putting feature into location quick features. Message: " + e.getMessage());
            return null;
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
            // The MFCC file:
            File mfccFile = _audioProcessor.getMFCCFile();
            if (!mfccFile.exists()) {
                Log.e(LOG_TAG,"data-zipping. MFCC file doesn't exist");
            }
            else {
                Log.i(LOG_TAG,"data-zipping. Adding MFCC file.");
                FileInputStream fileInputStream = new FileInputStream(mfccFile);
                byte[] buffer = new byte[2048];
                zos.putNextEntry(new ZipEntry(ESAudioProcessor.MFCC_FILENAME));
                int numBytes;
                while ((numBytes = fileInputStream.read(buffer)) > 0) {
                    //Log.d(LOG_TAG,"data-zipping. Reading " + numBytes + " from MFCC file to zip.");
                    zos.write(buffer,0,numBytes);
                }
                zos.closeEntry();
            }
            // The audio properties file:
            File audioPropFile = _audioProcessor.getAudioPropertiesFile();
            if (!audioPropFile.exists()) {
                Log.e(LOG_TAG,"data-zipping. Audio properties file doesn't exist.");
            }
            else {
                Log.i(LOG_TAG,"data-zipping. Adding audio properties file.");
                FileInputStream fileInputStream = new FileInputStream(audioPropFile);
                byte[] buffer = new byte[2048];
                zos.putNextEntry(new ZipEntry(ESAudioProcessor.AUDIO_PROPERTIES_FILENAME));
                int numBytes;
                while ((numBytes = fileInputStream.read(buffer)) > 0) {
                    zos.write(buffer,0,numBytes);
                }
                zos.closeEntry();
            }

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

    /**
     * Get a File object for the (possibly non-existing) zip file referring to the record with the given timestamp.
     * @param timestamp The timestamp identifying the record of interest
     * @return
     */
    public static File getZipFileForRecord(ESTimestamp timestamp) {
        return new File(ESApplication.getZipDir(),getZipFilename(timestamp));
    }

    public static String getZipFilename(ESTimestamp timestamp) {
        return timestamp.toString() + "-" + ESSettings.uuid() + ".zip";
    }

    private String currentZipFilename() {
        return getZipFilename(_timestamp);
    }

    private boolean checkIfShouldFinishSession() {
        if (_highFreqData == null) {
            Log.e(LOG_TAG,"high frequency data is null");
            return true;
        }

        // Check expected feature keys:
        for (String featureKey : _sensorKeysThatShouldGetEnoughSamples) {
            if (!_highFreqData.containsKey(featureKey) ||
                    _highFreqData.get(featureKey) == null ||
                    _highFreqData.get(featureKey).size() < NUM_SAMPLES_IN_SESSION) {
                // Then we should wait for this key's sensor to finish sampling
                return false;
            }
        }

        return true;
    }


    private void collectPhoneStateMeasurements() {
        // Wifi connectivity:
        try {
            _lowFreqData.put(WIFI_STATUS,ESNetworkAccessor.getESNetworkAccessor().isThereWiFiConnectivity());
        } catch (JSONException e) {
            Log.e(LOG_TAG,e.getMessage());
        }

        // On-the-phone:
        TelephonyManager telephonyManager = (TelephonyManager) ESApplication.getTheAppContext().getSystemService(Context.TELEPHONY_SERVICE);
        boolean onThePhone = (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE);
        try {
            _lowFreqData.put(ON_THE_PHONE,onThePhone);
        } catch (JSONException e) {
            Log.e(LOG_TAG,e.getMessage());
        }

        // Battery:
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = ESApplication.getTheAppContext().registerReceiver(null, intentFilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
        try {
            _lowFreqData.put(BATTERY_STATE,getStringValueForBatteryStatus(status));
        } catch (JSONException e) {
            Log.e(LOG_TAG,e.getMessage());
        }

        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
        try {
            _lowFreqData.put(BATTERY_PLUGGED,getStringValueForBatteryPlugged(plugged));
        } catch (JSONException e) {
            Log.e(LOG_TAG,e.getMessage());
        }

        int batteryLevelInt = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int batteryScaleInt = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        if (batteryLevelInt != -1 && batteryScaleInt != -1) {
            double batteryLevel = (double)batteryLevelInt / (double)batteryScaleInt;
            try {
                _lowFreqData.put(BATTERY_LEVEL,batteryLevel);
            }
            catch (JSONException e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }

        // Ringer:
        AudioManager audioManager = (AudioManager) ESApplication.getTheAppContext().getSystemService(Context.AUDIO_SERVICE);
        try {
            _lowFreqData.put(RINGER_MODE,getStringValueForRingerMode(audioManager.getRingerMode()));
        }
        catch (JSONException e) {
            Log.e(LOG_TAG,e.getMessage());
        }

        // Time:
        try {
            int hour = _timestamp.getHourOfDayOutOf24();
            int minute = _timestamp.getMinuteOfHour();
            _lowFreqData.put(HOUR_OF_DAY,hour);
            _lowFreqData.put(MINUTE_IN_HOUR,minute);
            Log.d(LOG_TAG,"== Timestamp " + _timestamp + ": " + _timestamp.infoString());
            Log.d(LOG_TAG,"== Timestamp hour: " + hour + ". minute: " + minute);
        } catch (JSONException e) {
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    // Implementing the SensorEventListener interface:
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Sanity check: we shouldn't be recording now:
        if (!is_recordingRightNow()) {
            Log.e(LOG_TAG,"!!! We're not in a recording session (maybe finished recently) but got a sensor event for: " + event.sensor.getName());
            return;
        }
        boolean sensorCollectedEnough = false;
        double timestampSeconds =  ((double)event.timestamp) / NANOSECONDS_IN_SECOND;

        try {

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    addHighFrequencyMeasurement(RAW_ACC_X, event.values[0]);
                    addHighFrequencyMeasurement(RAW_ACC_Y, event.values[1]);
                    addHighFrequencyMeasurement(RAW_ACC_Z, event.values[2]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(RAW_ACC_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    addHighFrequencyMeasurement(RAW_MAGNET_X, event.values[0]);
                    addHighFrequencyMeasurement(RAW_MAGNET_Y, event.values[1]);
                    addHighFrequencyMeasurement(RAW_MAGNET_Z, event.values[2]);
                    addHighFrequencyMeasurement(RAW_MAGNET_BIAS_X, event.values[3]);
                    addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Y, event.values[4]);
                    addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Z, event.values[5]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(RAW_MAGNET_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    addHighFrequencyMeasurement(RAW_GYRO_X, event.values[0]);
                    addHighFrequencyMeasurement(RAW_GYRO_Y, event.values[1]);
                    addHighFrequencyMeasurement(RAW_GYRO_Z, event.values[2]);
                    addHighFrequencyMeasurement(RAW_GYRO_DRIFT_X, event.values[3]);
                    addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Y, event.values[4]);
                    addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Z, event.values[5]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(RAW_GYRO_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_GRAVITY:
                    addHighFrequencyMeasurement(PROC_GRAV_X, event.values[0]);
                    addHighFrequencyMeasurement(PROC_GRAV_Y, event.values[1]);
                    addHighFrequencyMeasurement(PROC_GRAV_Z, event.values[2]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(PROC_GRAV_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    addHighFrequencyMeasurement(PROC_ACC_X, event.values[0]);
                    addHighFrequencyMeasurement(PROC_ACC_Y, event.values[1]);
                    addHighFrequencyMeasurement(PROC_ACC_Z, event.values[2]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(PROC_ACC_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    addHighFrequencyMeasurement(PROC_MAGNET_X, event.values[0]);
                    addHighFrequencyMeasurement(PROC_MAGNET_Y, event.values[1]);
                    addHighFrequencyMeasurement(PROC_MAGNET_Z, event.values[2]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(PROC_MAGNET_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    addHighFrequencyMeasurement(PROC_GYRO_X, event.values[0]);
                    addHighFrequencyMeasurement(PROC_GYRO_Y, event.values[1]);
                    addHighFrequencyMeasurement(PROC_GYRO_Z, event.values[2]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(PROC_GYRO_TIME, timestampSeconds);
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    addHighFrequencyMeasurement(PROC_ROTATION_X, event.values[0]);
                    addHighFrequencyMeasurement(PROC_ROTATION_Y, event.values[1]);
                    addHighFrequencyMeasurement(PROC_ROTATION_Z, event.values[2]);
//                addHighFrequencyMeasurement(PROC_ROTATION_COS,event.values[4]);
//                addHighFrequencyMeasurement(PROC_ROTATION_ACCURACY,event.values[5]);
                    sensorCollectedEnough = addHighFrequencyMeasurement(PROC_ROTATION_TIME, timestampSeconds);
                    break;
                // Low frequency (one-time) sensors:
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    _lowFreqData.put(TEMPERATURE_AMBIENT, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_LIGHT:
                    _lowFreqData.put(LIGHT, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_PRESSURE:
                    _lowFreqData.put(PRESSURE, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_PROXIMITY:
                    _lowFreqData.put(PROXIMITY, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    _lowFreqData.put(HUMIDITY, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                default:
                    Log.e(LOG_TAG, "Got event from unsupported sensor with type " + event.sensor.getType());
            }

            finishIfTooMuchTimeRecording();
            if (sensorCollectedEnough) {
                // Then we've collected enough samples from accelerometer,
                // and we can stop listening to it.
                Log.d(LOG_TAG,"=========== unregistering sensor: " + event.sensor.getName());
                _sensorManager.unregisterListener(this, event.sensor);
                finishSessionIfReady();
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG,"Problem adding sensor measurement to json object. " + event.sensor.toString());
            e.printStackTrace();
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
        // Should we send the exact coordinates?
        if ((!ESSettings.shouldUseLocationBubble()) ||
                (ESSettings.locationBubbleCenter() == null) ||
                (ESSettings.locationBubbleCenter().distanceTo(location) > LOCATION_BUBBLE_RADIUS_METERS)) {
            Log.i(LOG_TAG, "Sending location coordinates");
            addHighFrequencyMeasurement(LOC_LAT, location.getLatitude());
            addHighFrequencyMeasurement(LOC_LONG,location.getLongitude());
        }
        else {
            Log.i(LOG_TAG,"Hiding location coordinates (sending invalid coordinates). We're in the bubble.");
            addHighFrequencyMeasurement(LOC_LAT,LOC_LAT_HIDDEN);
            addHighFrequencyMeasurement(LOC_LONG,LOC_LONG_HIDDEN);
        }
        // Anyway, store the location coordinates separately:
        _locationCoordinatesData.get(LOC_LAT).add(location.getLatitude());
        _locationCoordinatesData.get(LOC_LONG).add(location.getLongitude());

        addHighFrequencyMeasurement(LOC_HOR_ACCURACY,location.hasAccuracy() ? location.getAccuracy() : LOC_ACCURACY_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_ALT,location.hasAltitude() ? location.getAltitude() : LOC_ALT_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_SPEED,location.hasSpeed() ? location.getSpeed() : LOC_SPEED_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_BEARING,location.hasBearing() ? location.getBearing() : LOC_BEARING_UNAVAILABLE);
    }
}
