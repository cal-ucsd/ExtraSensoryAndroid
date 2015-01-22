package edu.ucsd.calab.extrasensory.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

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

    /**
     * Get the single instance of this class
     * @return
     */
    public static ESSensorManager getESSensorManager(Context context) {
        if (theSingleSensorManager == null) {
            theSingleSensorManager = new ESSensorManager(context);
        }

        return theSingleSensorManager;
    }


    // Non static part:
    private SensorManager _sensorManager;
    private Context _context;

    /**
     * Making the constructor private, in order to make this class a singleton
     */
    private ESSensorManager(Context context) {
        _context = context;
        _sensorManager = (SensorManager) _context.getSystemService(Context.SENSOR_SERVICE);
        Log.v(LOG_TAG,"An instance of ESSensorManager was created.");
    }

    public void startRecordingSensors() {
        Log.i(LOG_TAG,"Starting recording.");
    }

    public void stopRecordingSensors() {
        Log.i(LOG_TAG,"Stopping recording.");
    }

    // Implementing the SensorEventListener interface:
    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
