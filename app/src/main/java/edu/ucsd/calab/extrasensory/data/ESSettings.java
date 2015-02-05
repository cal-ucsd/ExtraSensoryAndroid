package edu.ucsd.calab.extrasensory.data;

import android.content.Context;

import edu.ucsd.calab.extrasensory.ESApplication;

/**
 * This class represents the global settings for the app,
 * some of which have to be fixed and some are mutable by the user interface.
 *
 * Created by Yonatan on 2/4/2015.
 */
public class ESSettings {

    private String _uuid;
    private int _maxStoredExamples;
    private int _notificationIntervalInSeconds;

    ESSettings(String uuid,int maxStoredExamples,int notificationIntervalInSeconds) {
        _uuid = uuid;
        _maxStoredExamples = maxStoredExamples;
        _notificationIntervalInSeconds = notificationIntervalInSeconds;
    }

    private static ESSettings _settings = null;
    private static Context _context = null;

    private static ESSettings getTheSettings() {
        if (_settings == null) {
            _context = ESApplication.getTheAppContext();
            _settings = ESDatabaseAccessor.getESDatabaseAccessor(_context).getTheSettings();
        }

        return _settings;
    }

    /**
     * Get the UUID (unique user identifier) of the application.
     * @return The UUID of the application
     */
    public static String uuid() {
        return getTheSettings()._uuid;
    }

    /**
     * Get the maximum allowed number of stored examples.
     * @return The maximum allowed number of stored examples
     */
    public static int maxStoredExamples() {
        return getTheSettings()._maxStoredExamples;
    }

    /**
     * Get the notification interval - the time interval between scheduled notification checks.
     * (in each check, if necessary a notification will pop to the user).
     * The interval is given in seconds.
     *
     * @return Notification interval (seconds).
     */
    public static int notificationIntervalInSeconds() {
        return getTheSettings()._notificationIntervalInSeconds;
    }

    /**
     * Set the maximum allowed number of stored examples.
     *
     * @param maxStoredExamples The maximum allowed number of stored examples.
     */
    public static void setMaxStoredExamples(int maxStoredExamples) {
        //TODO set record through dbAccessor and get from it the new ESSettings object
        //TODO set the singleton _settings to the received object
    }

    /**
     * Set the time interval (in seconds) between scheduled notification checks
     * (when checking if a user notification is needed).
     *
     * @param notificationIntervalInSeconds The notification interval (in seconds)
     */
    public static void setNotificationIntervalInSeconds(int notificationIntervalInSeconds) {
        //TODO set record through dbAccessor and get from it the new ESSettings object
        //TODO set the singleton _settings to the received object

    }
}
