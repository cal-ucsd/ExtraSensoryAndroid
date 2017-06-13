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
        public static final String COLUMN_NAME_PREDICTED_LABEL_NAMES_CSV = "predicted_label_names";
        public static final String COLUMN_NAME_PREDICTED_LABEL_PROBS_CSV = "predicted_label_probs";
    }

    /**
     * Column names for table ESSettings (supposed to contain exactly a single record)
     */
    public static abstract class ESSettingsEntry {
        public static final String TABLE_NAME = "es_settings";
        public static final String COLUMN_NAME_UUID = "uuid";
        public static final String COLUMN_NAME_MAX_STORED_EXAMPLES = "max_stored_examples";
        public static final String COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS = "notification_interval";
        public static final String COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND = "num_examples_store_before_send";
        public static final String COLUMN_NAME_HOME_SENSING = "home_sensing";
        public static final String COLUMN_NAME_ALLOW_CELLULAR = "allow_cellular";
        public static final String COLUMN_NAME_BUBBLE_USED = "bubble_used";
        public static final String COLUMN_NAME_BUBBLE_CENTER_LONG = "bubble_center_longitude";
        public static final String COLUMN_NAME_BUBBLE_CENTER_LAT = "bubble_center_latitude";
        public static final String COLUMN_NAME_CLASSIFIER_TYPE = "classifier_type";
        public static final String COLUMN_NAME_CLASSIFIER_NAME = "classifier_name";
    }
}
