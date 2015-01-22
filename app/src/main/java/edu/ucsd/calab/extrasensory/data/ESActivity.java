package edu.ucsd.calab.extrasensory.data;

/**
 * This class represents a single instance (roughly representing 1 minute) and describes the labels
 * relevant to this instance.
 *
 * Created by Yonatan on 1/16/2015.
 */
public class ESActivity {

    public enum ESLabelSource {
        ES_LABEL_SOURCE_DEFAULT,
        ES_LABEL_SOURCE_ACTIVE_START,
        ES_LABEL_SOURCE_ACTIVE_CONTINUE,
        ES_LABEL_SOURCE_HISTORY,
        ES_LABEL_SOURCE_NOTIFICATION_BLANK,
        ES_LABEL_SOURCE_NOTIFICATION_ANSWER_CORRECT,
        ES_LABEL_SOURCE_NOTIFICATION_ANSWER_NOT_EXACTLY
    }

    // Data members of ESActivity:
    private int _timestamp;
    private ESLabelSource _labelSource;
    private String _mainActivityServerPrediction;
    private String _mainActivityUserCorrection;
    private String[] _secondaryActivities;
    private String[] _moods;

    // Constructors available only inside the package:
    ESActivity(int timestamp) {
        _timestamp = timestamp;
        _labelSource = ESLabelSource.ES_LABEL_SOURCE_DEFAULT;
        _mainActivityServerPrediction = null;
        _mainActivityUserCorrection = null;
        _secondaryActivities = null;
        _moods = null;
    }

    ESActivity(int timestamp, ESLabelSource labelSource,
               String mainActivityServerPrediction, String mainActivityUserCorrection,
               String[] secondaryActivities, String[] moods) {
        _timestamp = timestamp;
        _labelSource = labelSource;
        _mainActivityServerPrediction = mainActivityServerPrediction;
        _mainActivityUserCorrection = mainActivityUserCorrection;
        _secondaryActivities = secondaryActivities;
        _moods = moods;
    }

    // Public getters:
    public int get_timestamp() {
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
