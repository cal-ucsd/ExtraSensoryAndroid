package edu.ucsd.calab.extrasensory.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import edu.ucsd.calab.extrasensory.R;


/**
 * This class should be the only interface for the entire application to manipulate data.
 * It is designed as a singleton, to prevent inconsistencies.
 *
 * Created by Yonatan on 1/16/2015.
 */
public class ESDatabaseAccessor {

    private static final String LOG_TAG = "[ESDatabaseAccessor]";

    private static ESDatabaseAccessor _theSingleAccessor;


    public ESDatabaseAccessor getESDatabaseAccessor(Context context) {
        if (_theSingleAccessor == null) {
            _theSingleAccessor = new ESDatabaseAccessor(context);
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

        public ESDBHelper(Context context) {
            super(context, context.getString(R.string.database_name),null,DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ES_ACTIVITY_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ES_ACTIVITY_TABLE);
            onCreate(db);
        }
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

        return newActivity;
    }

    /**
     * Get an instance of ESActivity corresponding to the given timestamp.
     * Read the corresponding record's properties from the DB and return an object with these properties.
     *
     * @param timestamp The timestamp for the activity instance.
     * @return The desired activity
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
            Log.e(LOG_TAG,"No matching ESActivity record for timestamp " + timestamp);
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
     * Make changes to the values of the properties of an activity instance.
     * These changes will be reflected both in the given ESActivity object
     * and in the corresponding record in the DB.
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
        //TODO set the properties to the ESActivity object, and update the appropriate record in db
        //TODO remember to copy the arrays and not simply shallow-copy their pointers

        SQLiteDatabase db = _dbHelper.getWritableDatabase();

        // Update the relevant DB record:
        ContentValues values = new ContentValues();
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_LABEL_SOURCE,labelSource.get_value());
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION,mainActivityServerPrediction);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION,mainActivityUserCorrection);
        String secondaryCSV = makeCSV(secondaryActivities);
        values.put(ESDatabaseContract.ESActivityEntry.COLUMN_NAME_SECONDARY_ACTIVITIES_CSV,secondaryCSV);
        String moodCSV = makeCSV(moods);
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
    public ESActivity[] getActivitiesFromTimeRange(ESTimestamp fromTimestamp,ESTimestamp toTimestamp) {
        //TODO retrieve the relevant records from the db, sort them and create objects for them.
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

    private String[] parseCSV(String csv) {
        return csv.split(",");
    }

    /**
     * This function assumes non of the components of the array are null
     * @param labels The labels to represent in a single String. Assumed that no string contains comma.
     * @return A single String representation of the array
     */
    private String makeCSV(String[] labels) {
        if (labels == null || labels.length <= 0) {
            return "";
        }
        String csv = labels[0];
        for (int i = 1; i < labels.length; i ++) {
            csv = csv + "," + labels[i];
        }

        return csv;
    }

    private String[] copyStringArray(String[] source) {
        if (source == null) {
            return null;
        }

        String[] destination = new String[source.length];
        System.arraycopy(source, 0, destination, 0, source.length);

        return destination;
    }
}
