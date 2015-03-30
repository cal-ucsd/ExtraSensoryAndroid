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

    public static class ESBubbleCenter {
        private double _locationBubbleCenterLat = -1.;
        private double _locationBubbleCenterLong = -1.;
        private ESBubbleCenter(double locationBubbleCenterLat,double locationBubbleCenterLong) {
            _locationBubbleCenterLat = locationBubbleCenterLat;
            _locationBubbleCenterLong = locationBubbleCenterLong;
        }
        public double get_locationBubbleCenterLat() {
            return _locationBubbleCenterLat;
        }
        public double get_locationBubbleCenterLong() {
            return _locationBubbleCenterLong;
        }
    }
    private String _uuid;
    private int _maxStoredExamples;
    private int _notificationIntervalInSeconds;
    private ESBubbleCenter _locationBubbleCenter;

    ESSettings(String uuid,int maxStoredExamples,int notificationIntervalInSeconds,
               double locationBubbleCenterLat,double locationBubbleCenterLong) {
        _uuid = uuid;
        _maxStoredExamples = maxStoredExamples;
        _notificationIntervalInSeconds = notificationIntervalInSeconds;
        _locationBubbleCenter = new ESBubbleCenter(locationBubbleCenterLat, locationBubbleCenterLong);
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
    public static ESBubbleCenter locationBubbleCenter() {
        ESBubbleCenter bubbleCenter = getTheSettings()._locationBubbleCenter;
        if (bubbleCenter._locationBubbleCenterLat < 0. || bubbleCenter._locationBubbleCenterLong < 0.) {
            return null;
        }
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
     * Set the location Bubble center (latitude and longitude)
     * @param locationBubbleCenterLat
     * @param locationBubbleCenterLong
     */
    public static void setLocationBubbleCenter(double locationBubbleCenterLat,double locationBubbleCenterLong) {
        _settings = getTheDBAccessor().setSettings(locationBubbleCenterLat,locationBubbleCenterLong);
    }

    private static ESDatabaseAccessor getTheDBAccessor() {
        return ESDatabaseAccessor.getESDatabaseAccessor();
    }

}
