package edu.ucsd.calab.extrasensory.data;

import android.util.Log;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a single instance (roughly representing 1 minute) and describes the labels
 * relevant to this instance.
 *
 * Created by Yonatan on 1/16/2015.
 */
public class ESActivity {

    private static final String LOG_TAG = "[ESActivity]";


    public enum ESLabelSource {
        ES_LABEL_SOURCE_DEFAULT(-1),
        ES_LABEL_SOURCE_ACTIVE_START(0),
        ES_LABEL_SOURCE_ACTIVE_CONTINUE(1),
        ES_LABEL_SOURCE_HISTORY(2),
        ES_LABEL_SOURCE_NOTIFICATION_BLANK(3),
        ES_LABEL_SOURCE_NOTIFICATION_ANSWER_CORRECT(4),
        ES_LABEL_SOURCE_NOTIFICATION_ANSWER_NOT_EXACTLY(5),
        ES_LABEL_SOURCE_NOTIFICATION_ANSWER_CORRECT_FROM_WATCH(6);

        private final int _value;
        private ESLabelSource(final int value) {
            _value = value;
        }

        private static Map<Integer, ESLabelSource> map = new HashMap<Integer, ESLabelSource>();

        static {
            for (ESLabelSource labelSource : ESLabelSource.values()) {
                map.put(labelSource._value, labelSource);
            }
        }

        static ESLabelSource labelSourceFromValue(int value) {
            if (map.containsKey(value)) {
                return map.get(value);
            }
            else {
                String msg = "Got unsupported label source value " + value;
                Log.e(LOG_TAG, msg);
                throw new InvalidParameterException(msg);
            }
        }

        int get_value() {
            return _value;
        }
    }

    // Data members of ESActivity:
    private ESTimestamp _timestamp;
    private ESLabelSource _labelSource;
    private String _mainActivityServerPrediction;
    private String _mainActivityUserCorrection;
    private String[] _secondaryActivities;
    private String[] _moods;
    private String[] _predictedLabelNames;
    private double[] _predictedLabelProbs;

    // Constructors available only inside the package:
    ESActivity(ESTimestamp timestamp) {
        _timestamp = timestamp;
        _labelSource = ESLabelSource.ES_LABEL_SOURCE_DEFAULT;
        _mainActivityServerPrediction = null;
        _mainActivityUserCorrection = null;
        _secondaryActivities = null;
        _moods = null;
        _predictedLabelNames = null;
        _predictedLabelProbs = null;
    }

    ESActivity(ESTimestamp timestamp, ESLabelSource labelSource,
               String mainActivityServerPrediction, String mainActivityUserCorrection,
               String[] secondaryActivities, String[] moods,
               String[] predictedLabelNames, double[] predictedLabelProbs) {
        _timestamp = timestamp;
        _labelSource = labelSource;
        _mainActivityServerPrediction = mainActivityServerPrediction;
        _mainActivityUserCorrection = mainActivityUserCorrection;
        _secondaryActivities = secondaryActivities;
        _moods = moods;
        if ((predictedLabelNames==null && predictedLabelProbs!=null) || (predictedLabelNames!=null && predictedLabelProbs==null)) {
            Log.w(LOG_TAG, "Trying to construct ESActivity with one of predictedLabelNames and predictedLabelProbs being null. Setting them both to null.");
            _predictedLabelNames = null;
            _predictedLabelProbs = null;
        }
        else if (predictedLabelNames.length != predictedLabelProbs.length) {
            Log.v(LOG_TAG,"Trying to construct ESActivity with inconsistent lengths of predictedLabelNames and predictedLabelProbs. Setting them both to null.");
            _predictedLabelNames = null;
            _predictedLabelProbs = null;
        }
        else {
            _predictedLabelNames = predictedLabelNames;
            _predictedLabelProbs = predictedLabelProbs;
        }
    }

    // Public getters:
    public ESTimestamp get_timestamp() {
        return _timestamp;
    }

    public ESLabelSource get_labelSource() {
        return _labelSource;
    }

    public String get_mainActivityServerPrediction() {
        return _mainActivityServerPrediction;
    }

    public String get_mainActivityUserCorrection() {
        return _mainActivityUserCorrection;
    }

    public String[] get_secondaryActivities() {
        return _secondaryActivities;
    }

    public String[] get_moods() {
        return _moods;
    }

    public String[] get_predictedLabelNames() { return _predictedLabelNames; }

    public double[] get_predictedLabelProbs() { return _predictedLabelProbs; }


    // Utility public info functions:
    public boolean hasUserProvidedLabels() {return hasUserCorrectedMainLabel(); }
    public boolean hasUserCorrectedMainLabel() {
        return (_mainActivityUserCorrection != null);
    }

    @Override
    public String toString() {
        return "<timestamp: " + _timestamp +
                ", label source: " + _labelSource +
                ", main activity prediction: " + _mainActivityServerPrediction +
                ",main activity correction: " + _mainActivityUserCorrection +
                ",secondary: {" + _secondaryActivities + "}" +
                ",mood: {" + _moods + "}" +
                ",predicted label names: {" + _predictedLabelNames + "}" +
                ",predicted label probs: {" + _predictedLabelProbs + "}" +
                ">";
    }

    public String mostUpToDateMainActivity() {
        if (hasUserCorrectedMainLabel()) {
            return _mainActivityUserCorrection;
        }
        else {
            return _mainActivityServerPrediction;
        }
    }

    // Setters available only inside the package:
    void set_labelSource(ESLabelSource _labelSource) {
        this._labelSource = _labelSource;
    }

    void set_mainActivityServerPrediction(String _mainActivityServerPrediction) {
        this._mainActivityServerPrediction = _mainActivityServerPrediction;
    }

    void set_mainActivityUserCorrection(String _mainActivityUserCorrection) {
        this._mainActivityUserCorrection = _mainActivityUserCorrection;
    }

    void set_secondaryActivities(String[] _secondaryActivities) {
        this._secondaryActivities = _secondaryActivities;
    }

    void set_moods(String[] _moods) {
        this._moods = _moods;
    }



}
