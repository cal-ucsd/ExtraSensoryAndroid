package edu.ucsd.calab.extrasensory.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;


/**
 * This class should be the only interface for the entire application to manipulate data.
 * It is designed as a singleton, to prevent inconsistencies.
 *
 * Created by Yonatan on 1/16/2015.
 */
public class ESDatabaseAccessor {

    private static final String LOG_TAG = "[ESDatabaseAccessor]";
    private static final int MAX_STORED_EXAMPLES_DEFAULT = 600;
    private static final boolean USE_NOTIFICATIONS_DEFAULT = true;
    private static final int NOTIFICATION_INTERVAL_DEFAULT = 1800;
    private static final int NUM_EXAMPLES_STORE_BEFORE_SEND_DEFAULT = 0;
    private static final boolean HOME_SENSING_DEFAULT = false;
    private static final boolean USE_CELLULAR_DATA_DEFAULT = false;
    private static final boolean USE_LOCATION_BUBBLE_DEFAULT = false;
    private static final double LOCATION_BUBBLE_CENTER_LONG_DEFAULT = 0.0;
    private static final double LOCATION_BUBBLE_CENTER_LAT_DEFAULT = 0.0;
    private static final String LOCATION_BUBBLE_LOCATION_PROVIDER = "BubbleCenter";
    private static final String CLASSIFIER_TYPE_DEFAULT = "es_mlp";
    private static final String CLASSIFIER_NAME_DEFAULT = "es6sensors";
    private static final boolean RECORD_AUDIO_DEFAULT = true;
    private static final boolean RECORD_LOCATION_DEFAULT = true;
    private static final boolean RECORD_WATCH_DEFAULT = false;
    private static final boolean SAVE_PREDICTION_FILES_DEFAULT = false;

    private static ArrayList<Integer> defaultHFSensorsToRecordJSONString() {
        ArrayList<Integer> highFreqSensorTypesToRecordDefault = new ArrayList<>(2);
        highFreqSensorTypesToRecordDefault.add(Sensor.TYPE_ACCELEROMETER);
        highFreqSensorTypesToRecordDefault.add(Sensor.TYPE_GYROSCOPE);
        return highFreqSensorTypesToRecordDefault;
    }

    private static ArrayList<Integer> defaultLFSensorsToRecordJSONString() {
        ArrayList<Integer> lowFreqSensorTypesToRecordDefault = new ArrayList<>(10);
        lowFreqSensorTypesToRecordDefault.add(Sensor.TYPE_AMBIENT_TEMPERATURE);
        lowFreqSensorTypesToRecordDefault.add(Sensor.TYPE_LIGHT);
        lowFreqSensorTypesToRecordDefault.add(Sensor.TYPE_PRESSURE);
        lowFreqSensorTypesToRecordDefault.add(Sensor.TYPE_PROXIMITY);
        lowFreqSensorTypesToRecordDefault.add(Sensor.TYPE_RELATIVE_HUMIDITY);
        return lowFreqSensorTypesToRecordDefault;
    }

    public static final String BROADCAST_DATABASE_RECORDS_UPDATED = "edu.ucsd.calab.extrasensory.broadcast.database_records_updated";
    public enum ESLabelType {
        ES_LABEL_TYPE_MAIN,
        ES_LABEL_TYPE_SECONDARY,
        ES_LABEL_TYPE_MOOD
    }

    private static ESDatabaseAccessor _theSingleAccessor;


    public static ESDatabaseAccessor getESDatabaseAccessor() {
        if (_theSingleAccessor == null) {
            _theSingleAccessor = new ESDatabaseAccessor(ESApplication.getTheAppContext());
        }

        return _theSingleAccessor;
    }

    // Data members:
    private Context _context;
    private ESDBHelper _dbHelper;
    private ESDatabaseAccessor(Context context) {
        _context = context;
        _dbHelper = new ESDBHelper(_context);
    }

    /**
     * This class will help the database accessor handle the SQL database.
     */
    private class ESDBHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 1;
        private static final String SQL_CREATE_ES_ACTIVITY_TABLE =
                "CREATE TABLE " + ESDatabaseContract.ESActivityEntry.TABLE_NAME +
                        " (" +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " INTEGER PRIMARY KEY," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE + " INTEGER," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION + " TEXT," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION + " TEXT," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV + " TEXT," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV + " TEXT," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV + " TEXT," +
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV + " TEXT" +
                        ")";
        private static final String SQL_DELETE_ES_ACTIVITY_TABLE =
                "DROP TABLE IF EXISTS " + ESDatabaseContract.ESActivityEntry.TABLE_NAME;

        private static final String SQL_CREATE_ES_SETTINGS_TABLE =
                "CREATE TABLE " + ESDatabaseContract.ESSettingsEntry.TABLE_NAME +
                        " (" +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID + " TEXT PRIMARY KEY," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_USE_NOTIFICATIONS + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_ALLOW_CELLULAR + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HOME_SENSING + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_USED + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LAT + " DOUBLE PRECISION," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LONG + " DOUBLE PRECISION," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_TYPE + " TEXT," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_NAME + " TEXT," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_AUDIO + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_LOCATION + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_WATCH + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HF_SENSOR_TYPES_TO_RECORD_JSON + " TEXT," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_LF_SENSOR_TYPES_TO_RECORD_JSON + " TEXT," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_SAVE_PREDICTION_FILES + " INTEGER" +
                        ")";
        private static final String SQL_DELETE_ES_SETTINGS_TABLE =
                "DROP TABLE IF EXISTS " + ESDatabaseContract.ESSettingsEntry.TABLE_NAME;

        public ESDBHelper(Context context) {
            super(context, context.getString(R.string.database_name),null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG,"ESDatabaseAccessor: onCreate");
            db.execSQL(SQL_CREATE_ES_ACTIVITY_TABLE);
            Log.d(LOG_TAG,"ESDatabaseAccessor: onCreate. after creating activity table, before creating settings table.");
            db.execSQL(SQL_CREATE_ES_SETTINGS_TABLE);
            Log.d(LOG_TAG,"ESDatabaseAccessor: onCreate. after creating activity table and settings table.");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ES_ACTIVITY_TABLE);
            db.execSQL(SQL_DELETE_ES_SETTINGS_TABLE);
            onCreate(db);
        }
    }

    // Settings:

    private static String intArrayToJsonStr(ArrayList<Integer> numbers) {
        if (numbers == null) {
            numbers = new ArrayList<>();
        }

        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < numbers.size(); i ++) {
            jsonArray.put(numbers.get(i).intValue());
        }
        return jsonArray.toString();
    }

    private static ArrayList<Integer> jsonArrayToIntArray(String jsonStr) {
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            ArrayList<Integer> numbers = new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i ++) {
                numbers.add(jsonArray.getInt(i));
            }
            return numbers;
        } catch (JSONException e) {
            Log.e(LOG_TAG,"Failed to parse JSON as array of integers: " + jsonStr);
            return new ArrayList<>();
        }
    }

    /**
     * Create a new settings record for the application (including generating a unique user identifier).
     * This should only be called when there is no current record in the settings table.
     * The created record should be the only record in that table.
     *
     * @return an ESSettings object to represent the settings record
     */
    private synchronized ESSettings createSettingsRecord() {
        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        String uuid = generateUUID();
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID,uuid);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES, MAX_STORED_EXAMPLES_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_USE_NOTIFICATIONS,USE_NOTIFICATIONS_DEFAULT ? 1 : 0);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS,NOTIFICATION_INTERVAL_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND,NUM_EXAMPLES_STORE_BEFORE_SEND_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HOME_SENSING,HOME_SENSING_DEFAULT ? 1 : 0);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_ALLOW_CELLULAR,USE_CELLULAR_DATA_DEFAULT ? 1 : 0);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_USED,USE_LOCATION_BUBBLE_DEFAULT ? 1 : 0);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LAT,LOCATION_BUBBLE_CENTER_LAT_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LONG,LOCATION_BUBBLE_CENTER_LONG_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_TYPE,CLASSIFIER_TYPE_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_NAME,CLASSIFIER_NAME_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_AUDIO,RECORD_AUDIO_DEFAULT ? 1 : 0);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_LOCATION,RECORD_LOCATION_DEFAULT ? 1 : 0);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_WATCH,RECORD_WATCH_DEFAULT ? 1 : 0);
        ArrayList<Integer> defaultHFSensors = defaultHFSensorsToRecordJSONString();
        ArrayList<Integer> defaultLFSensors = defaultLFSensorsToRecordJSONString();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HF_SENSOR_TYPES_TO_RECORD_JSON,intArrayToJsonStr(defaultHFSensors));
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_LF_SENSOR_TYPES_TO_RECORD_JSON,intArrayToJsonStr(defaultLFSensors));
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_SAVE_PREDICTION_FILES,SAVE_PREDICTION_FILES_DEFAULT ? 1 : 0);

        db.insert(ESDatabaseContract.ESSettingsEntry.TABLE_NAME,null,values);
        Location bubbleCenter = new Location(LOCATION_BUBBLE_LOCATION_PROVIDER);
        bubbleCenter.setLatitude(LOCATION_BUBBLE_CENTER_LAT_DEFAULT);
        bubbleCenter.setLongitude(LOCATION_BUBBLE_CENTER_LONG_DEFAULT);
        ESSettings settings = new ESSettings(
                uuid,
                MAX_STORED_EXAMPLES_DEFAULT,
                USE_NOTIFICATIONS_DEFAULT,NOTIFICATION_INTERVAL_DEFAULT,
                NUM_EXAMPLES_STORE_BEFORE_SEND_DEFAULT,
                HOME_SENSING_DEFAULT,USE_CELLULAR_DATA_DEFAULT,USE_LOCATION_BUBBLE_DEFAULT,
                bubbleCenter,
                CLASSIFIER_TYPE_DEFAULT,CLASSIFIER_NAME_DEFAULT,
                RECORD_AUDIO_DEFAULT,RECORD_LOCATION_DEFAULT,RECORD_WATCH_DEFAULT,
                defaultHFSensors,defaultLFSensors,
                SAVE_PREDICTION_FILES_DEFAULT
                );

        _dbHelper.close();

        return settings;
    }

    /**
     * Get the ESSettings object for the only record of settings in the DB.
     * If it wasn't created yet, create this record and get it.
     * @return the settings of the application
     */
    synchronized ESSettings getTheSettings() {
        // Get the records (there should be zero or one records):
        SQLiteDatabase db = _dbHelper.getReadableDatabase();

        String[] projection = {
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_USE_NOTIFICATIONS,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HOME_SENSING,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_ALLOW_CELLULAR,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_USED,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LAT,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LONG,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_TYPE,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_NAME,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_AUDIO,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_LOCATION,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_WATCH,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HF_SENSOR_TYPES_TO_RECORD_JSON,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_LF_SENSOR_TYPES_TO_RECORD_JSON,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_SAVE_PREDICTION_FILES
        };

        Cursor cursor = db.query(ESDatabaseContract.ESSettingsEntry.TABLE_NAME,
                projection,null,null,null,null,null);

        int count = cursor.getCount();
        if (count < 1) {
            Log.i(LOG_TAG,"There is no settings record yet. Creating one");
            cursor.close();
            _dbHelper.close();
            return createSettingsRecord();
        }
        if (count > 1) {
            String msg = "Found more than one record for setting";
            Log.e(LOG_TAG,msg);
        }

        cursor.moveToFirst();
        ESSettings settings = extractSettingsFromCurrentRecord(cursor);
        cursor.close();
        _dbHelper.close();

        return settings;
    }

    // Settings setters:

    synchronized  ESSettings setSettingsMaxStoredExamples(int maxStoredExamples) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES,maxStoredExamples);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsUseNotifications(boolean useNotifications) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_USE_NOTIFICATIONS,useNotifications ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsNotificationInterval(int notificationInterval) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS,notificationInterval);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsNumExamplesStoredBeforeSend(int numExamplesStoreBeforeSend) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND,numExamplesStoreBeforeSend);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsHomeSensing(boolean homeSensingUsed) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HOME_SENSING,homeSensingUsed ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized  ESSettings setSettingsAllowCellular(boolean allowCellular) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_ALLOW_CELLULAR,allowCellular ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsUseLocationBubble(boolean locationBubbleUsed) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_USED,locationBubbleUsed ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsLocationBubbleCenterCoordinates(double locationBubbleCenterLat, double locationBubbleCenterLong) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LAT,locationBubbleCenterLat);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LONG,locationBubbleCenterLong);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsLocationBubbleCenter(Location locationBubbleCenter) {
        double locationBubbleCenterLat = locationBubbleCenter == null ? LOCATION_BUBBLE_CENTER_LAT_DEFAULT : locationBubbleCenter.getLatitude();
        double locationBubbleCenterLong = locationBubbleCenter == null ? LOCATION_BUBBLE_CENTER_LONG_DEFAULT : locationBubbleCenter.getLongitude();

        return setSettingsLocationBubbleCenterCoordinates(locationBubbleCenterLat,locationBubbleCenterLong);
    }

    synchronized ESSettings setClassifierSettings(String classifierType,String classifierName) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_TYPE,classifierType);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_NAME,classifierName);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setRecordAudio(boolean recordAudio) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_AUDIO,recordAudio ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setRecordLocation(boolean recordLocation) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_LOCATION,recordLocation ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setRecordWatch(boolean recordWatch) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_WATCH,recordWatch ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setHighFreqSensorsToRecord(ArrayList<Integer> hfSensorTypesToRecord) {
        ContentValues values = new ContentValues();
        String hfSensorsJson = intArrayToJsonStr(hfSensorTypesToRecord);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HF_SENSOR_TYPES_TO_RECORD_JSON,hfSensorsJson);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setLowFreqSensorsToRecord(ArrayList<Integer> lfSensorTypesToRecord) {
        ContentValues values = new ContentValues();
        String lfSensorsJson = intArrayToJsonStr(lfSensorTypesToRecord);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_LF_SENSOR_TYPES_TO_RECORD_JSON,lfSensorsJson);
        return updateSettingsAndReturnUpdatedRecord(values);
    }

    synchronized ESSettings setSettingsSavePredictionFiles(boolean savePredictionFiles) {
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_SAVE_PREDICTION_FILES,savePredictionFiles ? 1 : 0);
        return updateSettingsAndReturnUpdatedRecord(values);
    }



    private synchronized ESSettings updateSettingsAndReturnUpdatedRecord(ContentValues values) {
        SQLiteDatabase db = _dbHelper.getWritableDatabase();
        int affectedCount = db.update(ESDatabaseContract.ESSettingsEntry.TABLE_NAME,values,null,null);
        if (affectedCount <= 0) {
            Log.e(LOG_TAG,"Settings update affected no records in the DB");
        }
        if (affectedCount > 1) {
            Log.e(LOG_TAG,"Settings update affected more than one record in the DB");
        }

        _dbHelper.close();

        return getTheSettings();
    }


    private String generateUUID() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * Extract the settings record from the current position of the cursor and construct an
     * ESSettings object from them.
     * @param cursor A cursor, assumed currently pointing at the record of ESSettings.
     * @return The ESSettings object
     */
    private synchronized ESSettings extractSettingsFromCurrentRecord(Cursor cursor) {
        if (cursor == null) {
            Log.e(LOG_TAG,"Given null cursor");
            return null;
        }

        String uuid = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID));
        int maxStored = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES));
        boolean useNotifications = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_USE_NOTIFICATIONS)) > 0;
        int notificationInterval = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS));
        int numExamplesStoreBeforeSend = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND));
        boolean homeSensingUsed = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HOME_SENSING)) > 0;
        boolean allowCellular = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_ALLOW_CELLULAR)) > 0;
        boolean locationBubbleUsed = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_USED)) > 0;
        double locationBubbleCenterLat = cursor.getDouble(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LAT));
        double locationBubbleCenterLong = cursor.getDouble(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_BUBBLE_CENTER_LONG));

        Location locationBubbleCenter = new Location(LOCATION_BUBBLE_LOCATION_PROVIDER);
        locationBubbleCenter.setLatitude(locationBubbleCenterLat);
        locationBubbleCenter.setLongitude(locationBubbleCenterLong);

        String classifierType = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_TYPE));
        String classifierName = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_CLASSIFIER_NAME));

        boolean recordAudio = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_AUDIO)) > 0;
        boolean recordLocation = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_LOCATION)) > 0;
        boolean recordWatch = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_RECORD_WATCH)) > 0;
        String hfSensorsJson = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_HF_SENSOR_TYPES_TO_RECORD_JSON));
        ArrayList<Integer> hfSensors = jsonArrayToIntArray(hfSensorsJson);
        String lfSensorsJson = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_LF_SENSOR_TYPES_TO_RECORD_JSON));
        ArrayList<Integer> lfSensors = jsonArrayToIntArray(lfSensorsJson);

        boolean savePredictionFiles = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_SAVE_PREDICTION_FILES)) > 0;

        return new ESSettings(uuid,maxStored,
                useNotifications,notificationInterval,
                numExamplesStoreBeforeSend,homeSensingUsed,allowCellular,
                locationBubbleUsed,locationBubbleCenter,
                classifierType,classifierName,
                recordAudio,recordLocation,recordWatch,
                hfSensors,lfSensors,
                savePredictionFiles);
    }






    /**
     * The public data interface:
     */

    /**
     * Create a new record of an activity instance (minute-activity),
     * and the corresponding instance of ESActivity.
     *
     * @return A new activity instance, with "now"'s timestamp and default values for all the properties.
     */
    public synchronized ESActivity createNewActivity() {
        ESTimestamp timestamp = new ESTimestamp();
        // Make sure there is not already an existing record for this timestamp:
        ESActivity existingActivity = getESActivity(timestamp);
        if (existingActivity != null) {
            Log.e(LOG_TAG,"Tried to create new activity with timestamp " + timestamp + " but there is already one.");
            return null;
        }

        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP,timestamp.get_secondsSinceEpoch());
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE, ESActivity.ESLabelSource.ES_LABEL_SOURCE_DEFAULT.get_value());
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION,(String)null);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION,(String)null);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV,"");
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV,"");
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV,"");
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV,"");

        db.insert(ESDatabaseContract.ESActivityEntry.TABLE_NAME,null,values);
        ESActivity newActivity = new ESActivity(timestamp);

        _dbHelper.close();

        sendBroadcastDatabaseUpdate();

        return newActivity;
    }

    /**
     * Get an instance of ESActivity corresponding to the given timestamp,
     * of null if no such record exists.
     * Read the corresponding record's properties from the DB and return an object with these properties.
     *
     * @param timestamp The timestamp for the activity instance.
     * @return The desired activity, or null if there is no record for the timestamp.
     */
    public synchronized ESActivity getESActivity(ESTimestamp timestamp) {
        SQLiteDatabase db = _dbHelper.getReadableDatabase();

        String[] projection = {
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV
        };

        String selection = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " = " + timestamp.get_secondsSinceEpoch();

        Cursor cursor = db.query(ESDatabaseContract.ESActivityEntry.TABLE_NAME,
                projection,selection,null,null,null,null);

        int count = cursor.getCount();
        if (count < 1) {
            Log.i(LOG_TAG,"No matching ESActivity record for timestamp " + timestamp);
            return null;
        }
        if (count > 1) {
            String msg = "Found more than one record for timestamp " + timestamp;
            Log.e(LOG_TAG,msg);
        }

        cursor.moveToFirst();
        ESActivity activity = extractActivityFromCurrentRecord(cursor);
        cursor.close();
        _dbHelper.close();

        return activity;
    }

    /**
     * Set the server prediction for this activity
     * @param activity The ESActivity to set the prediction for
     * @param mainActivityServerPrediction The server prediction to assign to the activity
     */
    public synchronized void setESActivityServerPrediction(ESActivity activity,String mainActivityServerPrediction,
                                                           String[] predictedLabelNames,double[] predictedLabelProbs) {
        setESActivityValuesAndPossiblySendFeedback(activity,
                activity.get_labelSource(),
                mainActivityServerPrediction,
                activity.get_mainActivityUserCorrection(),
                activity.get_secondaryActivities(),
                activity.get_moods(),
                predictedLabelNames,predictedLabelProbs,
                false);
    }

    /**
     * Make changes to the values of the properties of an activity instance.
     * These changes will be reflected both in the given ESActivity object
     * and in the corresponding record in the DB.
     * After setting the new values, this will trigger an API call to send the labels to the server.
     *
     * @param activity The activity instance to set
     * @param labelSource The label source value to assign to the activity
     * @param mainActivityUserCorrection The user correction to assign to the activity
     * @param secondaryActivities The array of secondary activities to assign to the activity
     * @param moods The array of moods to assign to the activity
     */
    public synchronized void setESActivityValues(ESActivity activity,
                                                 ESActivity.ESLabelSource labelSource,String mainActivityUserCorrection,
                                                 String[] secondaryActivities,String[] moods) {
        setESActivityUserCorrectedValuesAndPossiblySendFeedback(activity,labelSource,mainActivityUserCorrection,
                secondaryActivities,moods,true);
    }

    /**
     * Make changes to the values of the properties of an activity instance (except the server-predicted values).
     * These changes will be reflected both in the given ESActivity object
     * and in the corresponding record in the DB.
     * IFF sendFeedback: after setting the new values, this will trigger an API call to send the labels to the server.
     *
     * @param activity The activity instance to set
     * @param labelSource The label source value to assign to the activity
     * @param mainActivityUserCorrection The user correction to assign to the activity
     * @param secondaryActivities The array of secondary activities to assign to the activity
     * @param moods The array of moods to assign to the activity
     * @param sendFeedback Should we send feedback update with this activity's labels?
     */
    public synchronized void setESActivityUserCorrectedValuesAndPossiblySendFeedback(ESActivity activity, ESActivity.ESLabelSource labelSource,
                                                                                     String mainActivityUserCorrection,
                                                                                     String[] secondaryActivities, String[] moods,
                                                                                     boolean sendFeedback) {

        setESActivityValuesAndPossiblySendFeedback(activity,labelSource,
                activity.get_mainActivityServerPrediction(),mainActivityUserCorrection,
                secondaryActivities,moods,
                activity.get_predictedLabelNames(),activity.get_predictedLabelProbs(),sendFeedback);
    }

    /**
     * Make changes to the values of the properties of an activity instance.
     * These changes will be reflected both in the given ESActivity object
     * and in the corresponding record in the DB.
     * IFF sendFeedback: after setting the new values, this will trigger an API call to send the labels to the server.
     *
     * @param activity The activity instance to set
     * @param labelSource The label source value to assign to the activity
     * @param mainActivityServerPrediction The server prediction to assign to the activity
     * @param mainActivityUserCorrection The user correction to assign to the activity
     * @param secondaryActivities The array of secondary activities to assign to the activity
     * @param moods The array of moods to assign to the activity
     * @param predictedLabelNames The array of labels provided by the server in the prediction
     * @param moods The array of prediction-probabilities assigned to the labels by the server
     * @param sendFeedback Should we send feedback update with this activity's labels?
     */
    public synchronized void setESActivityValuesAndPossiblySendFeedback(ESActivity activity,ESActivity.ESLabelSource labelSource,
                                                                        String mainActivityServerPrediction,String mainActivityUserCorrection,
                                                                        String[] secondaryActivities,String[] moods,
                                                                        String[] predictedLabelNames,double[] predictedLabelProbs,
                                                                        boolean sendFeedback) {

        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        // Update the relevant DB record:
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE,labelSource.get_value());
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION,mainActivityServerPrediction);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION,mainActivityUserCorrection);

        String secondaryCSV = ESLabelStrings.makeCSV(secondaryActivities);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV,secondaryCSV);

        String moodCSV = ESLabelStrings.makeCSV(moods);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV,moodCSV);

        String predictedLabelNamesCSV = ESLabelStrings.makeCSV(predictedLabelNames);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV,predictedLabelNamesCSV);

        String predictedLabelProbsCSV = ESLabelStrings.makeCSV(predictedLabelProbs);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV,predictedLabelProbsCSV);

        String selection = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " = " + activity.get_timestamp().get_secondsSinceEpoch();

        int affectedCount = db.update(ESDatabaseContract.ESActivityEntry.TABLE_NAME,values,selection,null);
        if (affectedCount <= 0) {
            Log.e(LOG_TAG,"Update didn't affect any records. Attempt for timestamp " + activity.get_timestamp());
        }
        if (affectedCount > 1) {
            Log.e(LOG_TAG,"Update affected " + affectedCount + " records. Timestamp " + activity.get_timestamp());
        }

        // Set the values of the ESActivity object:
        activity.set_labelSource(labelSource);
        activity.set_mainActivityServerPrediction(mainActivityServerPrediction);
        activity.set_mainActivityUserCorrection(mainActivityUserCorrection);
        activity.set_secondaryActivities(copyStringArray(secondaryActivities));
        activity.set_moods(copyStringArray(moods));

        _dbHelper.close();

        sendBroadcastDatabaseUpdate();

        if (sendFeedback) {
            // Since the labels of the activity were changed, send feedback API to the server:
            ESNetworkAccessor.getESNetworkAccessor().addToFeedbackQueue(activity);
        }
    }


    /**
     * Get all the activities from the given time range, already merged to continuous activities.
     * This method will extract the relevant information from the DB
     * and return an array of corresponding objects in ascending order of time (timestamp).
     *
     * @param fromTimestamp The earliest time in the desired range
     * @param toTimestamp The latest time in the desired range
     * @return An array of continuous activities from the desired time range, in ascending order of time
     */
    public synchronized ESContinuousActivity[] getContinuousActivitiesFromTimeRange(ESTimestamp fromTimestamp,ESTimestamp toTimestamp) {
        ESActivity[] minuteActivities = getActivitiesFromTimeRange(fromTimestamp,toTimestamp);
        return ESContinuousActivity.mergeContinuousActivities(minuteActivities);
    }

    /**
     * Get a single continuous activity, representing all the activities in a given time range.
     * Notice that this function doesn't merge activities according to their labels,
     * so it may result in a continuous activity containing activities with different labels.
     * You must be careful when using this function!!!
     * @param fromTimestamp The first timestamp in the desired time range
     * @param toTimestamp The last timestamp in the desired time range
     * @return A single continuous activity object, representing all the activities in the desired time range
     */
    public synchronized ESContinuousActivity getSingleContinuousActivityFromTimeRange(ESTimestamp fromTimestamp,ESTimestamp toTimestamp) {
        ESActivity[] minuteActivities = getActivitiesFromTimeRange(fromTimestamp,toTimestamp);
        return new ESContinuousActivity(minuteActivities);
    }

    /**
     * Get all the activities from the given time range.
     * This method will extract the relevant information from the DB
     * and return an array of corresponding objects in ascending order of time (timestamp).
     *
     * @param fromTimestamp The earliest time in the desired range
     * @param toTimestamp The latest time in the desired range
     * @return An array of the desired activities, sorted in ascending order of time.
     */
    private synchronized ESActivity[] getActivitiesFromTimeRange(ESTimestamp fromTimestamp,ESTimestamp toTimestamp) {
        if (fromTimestamp.isLaterThan(toTimestamp)) {
            // Then there should be no records in the range
            return new ESActivity[0];
        }

        SQLiteDatabase db = _dbHelper.getReadableDatabase();

        String[] projection = {
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV
        };

        String selection = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " >= " + fromTimestamp.get_secondsSinceEpoch() +
                " AND " + ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " <= " + toTimestamp.get_secondsSinceEpoch();

        String sortOrder = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " ASC";
        Cursor cursor = db.query(ESDatabaseContract.ESActivityEntry.TABLE_NAME,
                projection,selection,null,null,null,sortOrder);

        int count = cursor.getCount();
        ArrayList<ESActivity> activitiesList = new ArrayList<>(count);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            ESActivity activity = extractActivityFromCurrentRecord(cursor);
            activitiesList.add(activity);
        }
        cursor.close();

        ESActivity[] activities = activitiesList.toArray(new ESActivity[activitiesList.size()]);
        _dbHelper.close();

        return activities;
    }

    /**
     * Construct an ESActivity object representing the activity in a database record.
     * This method assumes the cursor is active and pointing at a record.
     *
     * @param cursor A cursor, currently pointing at the desired record.
     * @return An ESActivity object for the current record pointed to by the cursor.
     */
    private synchronized ESActivity extractActivityFromCurrentRecord(Cursor cursor) {
        if (cursor == null) {
            String msg = "null cursor given";
            Log.e(LOG_TAG, msg);
            throw new NullPointerException(msg);
        }

        int timestampSeconds = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP));
        ESTimestamp timestamp = new ESTimestamp(timestampSeconds);
        int labelSourceCode = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE));
        ESActivity.ESLabelSource labelSource = ESActivity.ESLabelSource.labelSourceFromValue(labelSourceCode);
        String serverMain = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION));
        String userMain = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION));

        String secondaryCSV = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV));
        String[] secondaryActivities = parsePossiblyEmptyCSV(secondaryCSV);
        //String[] secondaryActivities = (secondaryCSV==null || secondaryCSV.isEmpty()) ? new String[]{} : parseCSV(secondaryCSV);

        String moodCSV = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV));
        String[] moods = parsePossiblyEmptyCSV(moodCSV);
        //String[] moods = (moodCSV==null || moodCSV.isEmpty()) ? new String[]{} : parseCSV(moodCSV);

        String predictedLabelNamesCSV = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV));
        String[] predictedLabelNames = parsePossiblyEmptyCSV(predictedLabelNamesCSV);

        String predictedLabelProbsCSV = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV));
        double[] predictedLabelProbs = parseCSVOfNumbers(predictedLabelProbsCSV);

        return new ESActivity(timestamp,labelSource,serverMain,userMain,secondaryActivities,moods,predictedLabelNames,predictedLabelProbs);
    }

    /**
     * Split a continuous activity to separate continuous activity structures,
     * each representing just a single minute-activity.
     * @param continuousActivity The continuous activity to split
     * @return An array of continuous activities, each representing a single minute-activity
     */
    public ESContinuousActivity[] splitToSeparateContinuousActivities(ESContinuousActivity continuousActivity) {
        ESActivity[] minuteActivities = continuousActivity.getMinuteActivities();
        ESContinuousActivity[] splitActivities = new ESContinuousActivity[minuteActivities.length];
        for (int i=0; i<minuteActivities.length; i++) {
            splitActivities[i] = new ESContinuousActivity(new ESActivity[]{minuteActivities[i]});
        }

        return splitActivities;
    }

    /**
     * Get an array of the labels sorted in descending order of frequency of usage,
     * including only the labels that were actually used by the user.
     *
     * @param fromTime The earliest timepoint to count from, or null to count from all the history.
     * @param labelType either main, secondary or mood
     * @return The labels used in the time period, in descending order of frequency.
     */
    public synchronized String[] getFrequentlyUsedLabels(ESTimestamp fromTime,ESLabelType labelType) {
        // Get a label->counts map:
        Map<String,Integer> countsMap = getLabelCounts(fromTime, labelType);
        // Sort the label-count pairs according to count value:
        List<Map.Entry<String,Integer>> entryList = new LinkedList<>(countsMap.entrySet());
        Collections.sort(entryList,new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> lhs, Map.Entry<String, Integer> rhs) {
                // Make sure it's descending order:
                return rhs.getValue().compareTo(lhs.getValue());
            }
        });

        // Organize the array of sorted labels:
        String[] sortedLabels = new String[entryList.size()];
        for (int i=0; i<entryList.size(); i++) {
            Map.Entry<String,Integer> entry = entryList.get(i);
            Log.d(LOG_TAG,String.format("Frequently used. label: %s. Count: %d.",entry.getKey(),entry.getValue()));
            sortedLabels[i] = entry.getKey();
        }

        return sortedLabels;
    }

    /**
     * Get counts of user-provided labels.
     * For each label, how many times (how many minute activities) was this label reported.
     * @param fromTime The earliest timepoint to count from, or null to count from all the history.
     * @param labelType either main, secondary or mood
     * @return A map from label to count. Containing only the labels with non-zero counts.
     */
    public synchronized Map<String,Integer> getLabelCounts(ESTimestamp fromTime,ESLabelType labelType) {
        if (fromTime == null) {
            fromTime = new ESTimestamp(0);
        }
        ESActivity[] activities = getActivitiesFromTimeRange(fromTime,new ESTimestamp());
        HashMap<String,Integer> countsMap = new HashMap<>(10);

        switch (labelType) {
            case ES_LABEL_TYPE_MAIN:
                for (ESActivity activity : activities) {
                    String mainActivity = activity.get_mainActivityUserCorrection();
                    if (mainActivity != null) {
                        increaseLabelCount(countsMap,mainActivity);
                    }
                }
                break;
            case ES_LABEL_TYPE_SECONDARY:
                for (ESActivity activity : activities) {
                    String[] secondaries = activity.get_secondaryActivities();
                    if (secondaries != null) {
                        for (String secondary : secondaries) {
                            increaseLabelCount(countsMap,secondary);
                        }
                    }
                }
                break;
            case ES_LABEL_TYPE_MOOD:
                for (ESActivity activity : activities) {
                    String[] moods = activity.get_moods();
                    if (moods != null) {
                        for (String mood : moods) {
                            increaseLabelCount(countsMap,mood);
                        }
                    }
                }
                break;
        }

        return countsMap;
    }

    private void increaseLabelCount(HashMap<String,Integer> countsMap,String label) {
        if (!countsMap.containsKey(label)) {
            countsMap.put(label,0);
        }

        countsMap.put(label,countsMap.get(label) + 1);
    }

    /**
     * Go over the records from the starting time and later and check for orphan records:
     * records that have no server prediction, and no zip file related to them,
     * (and are not related to the "current" recording - so at least 1 minute old).
     * Then delete these orphan records.
     * @param fromTimestamp The earliest time in the desired range to check
     */
    public synchronized void clearOrphanRecords(ESTimestamp fromTimestamp) {
        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        String[] projection = {
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION
        };

        int aMinuteAgoInSecondsSinceEpoch = new ESTimestamp().get_secondsSinceEpoch() - 60;
        String selection = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " >= " + fromTimestamp.get_secondsSinceEpoch() +
                " AND " + ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " <= " + aMinuteAgoInSecondsSinceEpoch +
                " AND " + ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION + " IS NULL";

        String sortOrder = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " ASC";
        Cursor cursor = db.query(ESDatabaseContract.ESActivityEntry.TABLE_NAME,
                projection,selection,null,null,null,sortOrder);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int timestampSeconds = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP));

            // Check if this record has a corresponding zip file (if so, then it is still waiting to get server prediction from the server):
            File possibleZipFile = ESSensorManager.getZipFileForRecord(new ESTimestamp(timestampSeconds));
            if (possibleZipFile.exists()) {
                // Then we're still waiting to get server prediction for this record, and we shouldn't delete it.
                continue;
            }
            else {
                // Then probably this record represents a recording session that never finished,
                // and we can get rid of it:
                String whereToDelete = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " = " + timestampSeconds;
                db.delete(ESDatabaseContract.ESActivityEntry.TABLE_NAME,whereToDelete,null);
            }


        }
        cursor.close();

        _dbHelper.close();

    }

    /**
     * Get the latest activity that has user provided labels.
     * @param startFrom The earliest timestamp to check from
     * @return The latest verified activity, or null if no such activity was found in the desired time range.
     */
    public synchronized ESActivity getLatestVerifiedActivity(ESTimestamp startFrom) {
        ESActivity[] recentActivities = getActivitiesFromTimeRange(startFrom,new ESTimestamp());
        for (int i = recentActivities.length - 1; i >= 0; i --) {
            if (recentActivities[i].hasUserProvidedLabels()) {
                return recentActivities[i];
            }
        }

        return null;
    }


    private static String[] parseCSV(String csv) {
        return csv.split(",");
    }

    private static String[] parsePossiblyEmptyCSV(String csv) {
        if (csv==null || csv.isEmpty()) {
            return new String[]{};
        }
        return parseCSV(csv);
    }

    private static double[] parseCSVOfNumbers(String csv) {
        String[] numberStrings = parsePossiblyEmptyCSV(csv);
        double[] numbers = new double[numberStrings.length];
        for (int i = 0; i < numbers.length; i ++) {
            numbers[i] = Double.parseDouble(numberStrings[i]);
        }
        return numbers;
    }


    private String[] copyStringArray(String[] source) {
        if (source == null) {
            return null;
        }

        String[] destination = new String[source.length];
        System.arraycopy(source, 0, destination, 0, source.length);

        return destination;
    }

    /**
     * Announce, to whoever is interested, that there was some update to some ESActivity record in the DB.
     */
    private void sendBroadcastDatabaseUpdate() {
        Intent intent = new Intent(BROADCAST_DATABASE_RECORDS_UPDATED);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(_context);
        localBroadcastManager.sendBroadcast(intent);
    }
}
