package edu.ucsd.calab.extrasensory.data;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import edu.ucsd.calab.extrasensory.ESApplication;

/**
 * This class represents the global settings for the app,
 * some of which have to be fixed and some are mutable by the user interface.
 *
 * Created by Yonatan on 2/4/2015.
 */
public class ESSettings {

    private static final String LOG_TAG = "[ESSettings]";

    private String _uuid;
    private int _maxStoredExamples;
    private int _notificationIntervalInSeconds;
    private boolean _locationBubbleUsed;
    private Location _locationBubbleCenter;

    ESSettings(String uuid,int maxStoredExamples,int notificationIntervalInSeconds,
               boolean locationBubbleUsed, Location locationBubbleCenter) {
        _uuid = uuid;
        _maxStoredExamples = maxStoredExamples;
        _notificationIntervalInSeconds = notificationIntervalInSeconds;
        _locationBubbleUsed = locationBubbleUsed;
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
     * Should we use a location bubble?
     * @return
     */
    public static boolean shouldUseLocationBubble() { return getTheSettings()._locationBubbleUsed; }

    /**
     * Get the location bubble center - representing the home of the user,
     * a location that within a ball around it we shouldn't expose actual latitude/longitute values through the network.
     *
     * @return The location of the bubble center.
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
        ((ESApplication)ESApplication.getTheAppContext()).checkShouldWeCollectDataAndManageAppropriately();
    }

    public static void setLocationBubbleUsed(boolean useLocationBubble) {
        _settings = getTheDBAccessor().setSettings(useLocationBubble);
    }

    /**
     * Set the location bubble center by providing the coordinates of the center
     * @param locationBubbleCenterLat The latitude coordinate
     * @param locationBubbleCenterLong The longitude coordinate
     */
    public static void setLocationBubbleCenter(double locationBubbleCenterLat, double locationBubbleCenterLong) {
        _settings = getTheDBAccessor().setSettings(locationBubbleCenterLat,locationBubbleCenterLong);
        Log.i(LOG_TAG,String.format("Changed location bubble center to: <lat=%f,long=%f>",locationBubbleCenterLat,locationBubbleCenterLong));
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
