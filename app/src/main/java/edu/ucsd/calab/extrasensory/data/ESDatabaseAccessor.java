package edu.ucsd.calab.extrasensory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import edu.ucsd.calab.extrasensory.R;


/**
 * This class should be the only interface for the entire application to manipulate data.
 * It is designed as a singleton, to prevent inconsistencies.
 *
 * Created by Yonatan on 1/16/2015.
 */
public class ESDatabaseAccessor {

    private static final int DATABASE_VERSION = 1;

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
        //TODO  get timestamp, create new record in db table and create new instance.
        return null;
    }

    /**
     * Get an instance of ESActivity corresponding to the given timestamp.
     * Read the corresponding record's properties from the DB and return an object with these properties.
     *
     * @param timestamp The timestamp for the activity instance.
     * @return The desired activity
     */
    public ESActivity getESActivity(int timestamp) {
        //TODO retrieve the specific instance from the db
        return null;
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
    public ESActivity[] getActivitiesFromTimeRange(int fromTimestamp,int toTimestamp) {
        //TODO retrieve the relevant records from the db, sort them and create objects for them.
        return null;
    }
}
