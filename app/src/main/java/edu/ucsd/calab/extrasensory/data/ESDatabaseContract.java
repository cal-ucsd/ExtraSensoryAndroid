package edu.ucsd.calab.extrasensory.data;

/**
 * This class provides the contract for our database, the definitions of schema, tables and columns.
 *
 * Created by Yonatan on 1/16/2015.
 */
final class ESDatabaseContract {

    /**
     * Empty constructor. No one should instantiate this class.
     */
    public ESDatabaseContract() {}

    /**
     * Column names for table ESActivity
     */
    public static abstract class ESActivityEntry {
        public static final String TABLE_NAME = "es_activity";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_MAIN_ACTIVITY_SERVER_PREDICTION = "main_server_pred";
        public static final String COLUMN_NAME_MAIN_ACTIVITY_USER_CORRECTION = "main_user_correct";
        public static final String COLUMN_NAME_SECONDARY_ACTIVITIES_CSV = "secondary_activities";
        public static final String COLUMN_NAME_MOODS_CSV = "moods";
        public static final String COLUMN_NAME_LABEL_SOURCE = "label_source";
    }
}
