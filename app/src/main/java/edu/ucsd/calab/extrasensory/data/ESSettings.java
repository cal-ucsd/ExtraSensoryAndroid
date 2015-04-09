package edu.ucsd.calab.extrasensory.data;

import android.content.Context;
import android.location.Location;

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
    private Location _locationBubbleCenter;

    ESSettings(String uuid,int maxStoredExamples,int notificationIntervalInSeconds,
               Location locationBubbleCenter) {
        _uuid = uuid;
        _maxStoredExamples = maxStoredExamples;
        _notificationIntervalInSeconds = notificationIntervalInSeconds;
        _locationBubbleCenter = locationBubbleCenter;
    }

    private static ESSettings _settings = null;

    private static ESSettings getTheSettings() {
        if (_settings == null) {
            _settings = getTheDBAccessor().getTheSettings();
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
     * Get the location bubble center - representing the home of the user,
     * a location that within a ball around it we shouldn't expose actual latitude/longitute values through the network.
     *
     * @return The location of the bubble center, or null if there is no bubble requirement.
     */
    public static Location locationBubbleCenter() {
        return getTheSettings()._locationBubbleCenter;
    }

    /**
     * Set the maximum allowed number of stored examples.
     *
     * @param maxStoredExamples The maximum allowed number of stored examples.
     */
    public static void setMaxStoredExamples(int maxStoredExamples) {
        _settings = getTheDBAccessor().setSettings(maxStoredExamples,notificationIntervalInSeconds());
    }

    /**
     * Set the time interval (in seconds) between scheduled notification checks
     * (when checking if a user notification is needed).
     *
     * @param notificationIntervalInSeconds The notification interval (in seconds)
     */
    public static void setNotificationIntervalInSeconds(int notificationIntervalInSeconds) {
        _settings = getTheDBAccessor().setSettings(maxStoredExamples(),notificationIntervalInSeconds);
    }

    /**
     * Set the location of the bubble center
     * @param locationBubbleCenter
     */
    public static void setLocationBubbleCenter(Location locationBubbleCenter) {
        _settings = getTheDBAccessor().setSettings(locationBubbleCenter);
    }

    private static ESDatabaseAccessor getTheDBAccessor() {
        return ESDatabaseAccessor.getESDatabaseAccessor();
    }

}
