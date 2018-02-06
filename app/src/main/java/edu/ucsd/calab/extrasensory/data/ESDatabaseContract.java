package edu.ucsd.calab.extrasensory.data;

/**
 * This class provides the contract for our database, the definitions of schema, tables and columns.
 *
 * Created by Yonatan on 1/16/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
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
        public static final String COLUMN_NAME_LOCATION_REPRESENTATIVE_LAT_LONG_CSV = "location_rep_lat_long";
        public static final String COLUMN_NAME_TIMESTAMP_OPEN_FEEDBACK_FORM = "timestamp_open_feedback_form";
        public static final String COLUMN_NAME_TIMESTAMP_PRESS_SEND_BUTTON = "timestamp_press_send_feedback_button";
        public static final String COLUMN_NAME_TIMESTAMP_NOTIFICATION = "timestamp_notification";
        public static final String COLUMN_NAME_TIMESTAMP_USER_RESPOND_TO_NOTIFICATION = "timestamp_user_respond_to_notification";
    }

    /**
     * Column names for table ESSettings (supposed to contain exactly a single record)
     */
    public static abstract class ESSettingsEntry {
        public static final String TABLE_NAME = "es_settings";
        public static final String COLUMN_NAME_UUID = "uuid";
        public static final String COLUMN_NAME_MAX_STORED_EXAMPLES = "max_stored_examples";
        public static final String COLUMN_NAME_USE_NEAR_PAST_NOTIFICATIONS = "use_near_past_notifications";
        public static final String COLUMN_NAME_USE_NEAR_FUTURE_NOTIFICATIONS = "use_near_future_notifications";
        public static final String COLUMN_NAME_NOTIFICATION_INTERVAL_SECONDS = "notification_interval";
        public static final String COLUMN_NAME_NUM_EXAMPLES_STORE_BEFORE_SEND = "num_examples_store_before_send";
        public static final String COLUMN_NAME_ALLOW_CELLULAR = "allow_cellular";
        public static final String COLUMN_NAME_BUBBLE_USED = "bubble_used";
        public static final String COLUMN_NAME_BUBBLE_CENTER_LONG = "bubble_center_longitude";
        public static final String COLUMN_NAME_BUBBLE_CENTER_LAT = "bubble_center_latitude";
        public static final String COLUMN_NAME_CLASSIFIER_TYPE = "classifier_type";
        public static final String COLUMN_NAME_CLASSIFIER_NAME = "classifier_name";
        public static final String COLUMN_NAME_RECORD_AUDIO = "record_audio";
        public static final String COLUMN_NAME_RECORD_LOCATION = "record_location";
        public static final String COLUMN_NAME_RECORD_WATCH = "record_watch";
        public static final String COLUMN_NAME_HF_SENSOR_TYPES_TO_RECORD_JSON = "hf_sensors_to_record";
        public static final String COLUMN_NAME_LF_SENSOR_TYPES_TO_RECORD_JSON = "lf_sensors_to_record";
        public static final String COLUMN_NAME_SAVE_PREDICTION_FILES = "save_prediction_files";
        public static final String COLUMN_NAME_SAVE_USER_LABELS_FILES = "save_user_labels_files";
        public static final String COLUMN_NAME_HISTORY_TIME_UNIT_MINUTES = "history_basic_time_unit_in_minutes";
    }
}
