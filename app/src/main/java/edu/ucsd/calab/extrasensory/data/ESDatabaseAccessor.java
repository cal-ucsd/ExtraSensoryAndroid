package edu.ucsd.calab.extrasensory.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
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
    private static final int MAX_STORED_EXAMPLES_DEFAULT = 120;
    private static final int NOTIFICATION_INTERVAL_DEFAULT = 600;

    public static final String BROADCAST_DATABASE_RECORDS_UPDATED = "edu.ucsd.calab.extrasensory.broadcast.database_records_updated";


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
                        ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV + " TEXT" +
                        ")";
        private static final String SQL_DELETE_ES_ACTIVITY_TABLE =
                "DROP TABLE IF EXISTS " + ESDatabaseContract.ESActivityEntry.TABLE_NAME;

        private static final String SQL_CREATE_ES_SETTINGS_TABLE =
                "CREATE TABLE " + ESDatabaseContract.ESSettingsEntry.TABLE_NAME +
                        " (" +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID + " TEXT PRIMARY KEY," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES + " INTEGER," +
                        ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS + " INTEGER" +
                        ")";
        private static final String SQL_DELETE_ES_SETTINGS_TABLE =
                "DROP TABLE IF EXISTS " + ESDatabaseContract.ESSettingsEntry.TABLE_NAME;

        public ESDBHelper(Context context) {
            super(context, context.getString(R.string.database_name),null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ES_ACTIVITY_TABLE);
            db.execSQL(SQL_CREATE_ES_SETTINGS_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ES_ACTIVITY_TABLE);
            db.execSQL(SQL_DELETE_ES_SETTINGS_TABLE);
            onCreate(db);
        }
    }

    // Settings:

    /**
     * Create a new settings record for the application (including generating a unique user identifier).
     * This should only be called when there is no current record in the settings table.
     * The created record should be the only record in that table.
     *
     * @return an ESSettings object to represent the settings record
     */
    private ESSettings createSettingsRecord() {
        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        String uuid = generateUUID();
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID,uuid);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES, MAX_STORED_EXAMPLES_DEFAULT);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS,NOTIFICATION_INTERVAL_DEFAULT);

        long rowID = db.insert(ESDatabaseContract.ESSettingsEntry.TABLE_NAME,null,values);
        ESSettings settings = new ESSettings(uuid, MAX_STORED_EXAMPLES_DEFAULT,NOTIFICATION_INTERVAL_DEFAULT);

        _dbHelper.close();

        return settings;
    }

    /**
     * Get the ESSettings object for the only record of settings in the DB.
     * If it wasn't created yet, create this record and get it.
     * @return the settings of the application
     */
    ESSettings getTheSettings() {
        // Get the records (there should be zero or one records):
        SQLiteDatabase db = _dbHelper.getReadableDatabase();

        String[] projection = {
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES,
                ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS
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

    ESSettings setSettings(int maxStoredExamples,int notificationInterval) {
        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES,maxStoredExamples);
        values.put(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS,notificationInterval);

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
    private ESSettings extractSettingsFromCurrentRecord(Cursor cursor) {
        if (cursor == null) {
            Log.e(LOG_TAG,"Given null cursor");
            return null;
        }

        String uuid = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_UUID));
        int maxStored = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_MAX_STORED_EXAMPLES));
        int notificationInterval = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESSettingsEntry.COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS));

        return new ESSettings(uuid,maxStored,notificationInterval);
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
    public ESActivity createNewActivity() {
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

        long rowID = db.insert(ESDatabaseContract.ESActivityEntry.TABLE_NAME,null,values);
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
    public ESActivity getESActivity(ESTimestamp timestamp) {
        SQLiteDatabase db = _dbHelper.getReadableDatabase();

        String[] projection = {
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV,
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV
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
    public void setESActivityServerPrediction(ESActivity activity,String mainActivityServerPrediction) {
        setESActivityValuesAndPossiblySendFeedback(activity,
                activity.get_labelSource(),
                mainActivityServerPrediction,
                activity.get_mainActivityUserCorrection(),
                activity.get_secondaryActivities(),
                activity.get_moods(),false);
    }

    /**
     * Make changes to the values of the properties of an activity instance.
     * These changes will be reflected both in the given ESActivity object
     * and in the corresponding record in the DB.
     * After setting the new values, this will trigger an API call to send the labesl to the server.
     *
     * @param activity The activity instance to set
     * @param labelSource The label source value to assign to the activity
     * @param mainActivityServerPrediction The server prediction to assign to the activity
     * @param mainActivityUserCorrection The user correction to assign to the activity
     * @param secondaryActivities The array of secondary activities to assign to the activity
     * @param moods The array of moods to assign to the activity
     */
    public void setESActivityValues(ESActivity activity,ESActivity.ESLabelSource labelSource,
                                    String mainActivityServerPrediction,String mainActivityUserCorrection,
                                    String[] secondaryActivities,String[] moods) {
        setESActivityValuesAndPossiblySendFeedback(activity,labelSource,mainActivityServerPrediction,mainActivityUserCorrection,
                secondaryActivities,moods,true);
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
     * @param sendFeedback Should we send feedback update with this activity's labels?
     */
    public void setESActivityValuesAndPossiblySendFeedback(ESActivity activity,ESActivity.ESLabelSource labelSource,
                                                           String mainActivityServerPrediction,String mainActivityUserCorrection,
                                                           String[] secondaryActivities,String[] moods,
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
    public ESContinuousActivity[] getContinuousActivitiesFromTimeRange(ESTimestamp fromTimestamp,ESTimestamp toTimestamp) {
        ESActivity[] minuteActivities = getActivitiesFromTimeRange(fromTimestamp,toTimestamp);
        return ESContinuousActivity.mergeContinuousActivities(minuteActivities);
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
    private ESActivity[] getActivitiesFromTimeRange(ESTimestamp fromTimestamp,ESTimestamp toTimestamp) {
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
                ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV
        };

        String selection = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " >= " + fromTimestamp.get_secondsSinceEpoch() +
                " AND " + ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " <= " + toTimestamp.get_secondsSinceEpoch();

        String sortOrder = ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP + " ASC";
        Cursor cursor = db.query(ESDatabaseContract.ESActivityEntry.TABLE_NAME,
                projection,selection,null,null,null,sortOrder);

        int count = cursor.getCount();
        ArrayList<ESActivity> activitiesList = new ArrayList<ESActivity>(count);
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
    private ESActivity extractActivityFromCurrentRecord(Cursor cursor) {
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
        String[] secondaryActivities = parseCSV(secondaryCSV);
        String moodCSV = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MOODS_CSV));
        String[] moods = parseCSV(moodCSV);

        return new ESActivity(timestamp,labelSource,serverMain,userMain,secondaryActivities,moods);
    }

    /**
     * Go over the records from the starting time and later and check for orphan records:
     * records that have no server prediction, and no zip file related to them,
     * (and are not related to the "current" recording - so at least 1 minute old).
     * Then delete these orphan records.
     * @param fromTimestamp The earliest time in the desired range to check
     */
    public void clearOrphanRecords(ESTimestamp fromTimestamp) {
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

        int count = cursor.getCount();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int timestampSeconds = cursor.getInt(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_TIMESTAMP));
            String serverMain = cursor.getString(cursor.getColumnIndexOrThrow(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION));

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


    private String[] parseCSV(String csv) {
        return csv.split(",");
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
