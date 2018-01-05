package edu.ucsd.calab.extrasensory.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESDataFilesAccessor;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * This class handles the networking with the server.
 * The two main features are:
 * 1) uploading a sensor-measurements bundle to the server (and getting server predictions in return)
 * 2) and sending user label-feedback to the server.
 *
 * This class is designed as a singleton.
 * We need a single network agent to know the current state
 * (e.g. waiting for server response, so not available to upload new examples).
 *
 * Created by Yonatan on 1/17/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESNetworkAccessor {

    public static final String BROADCAST_NETWORK_QUEUE_SIZE_CHANGED = "edu.ucsd.calab.extrasensory.broadcast.network_queue_size_changed";
    public static final String BROADCAST_FEEDBACK_QUEUE_SIZE_CHANGED = "edu.ucsd.calab.extrasensory.broadcast.feedback_queue_size_changed";

    private static final String LOG_TAG = "[ESNetworkAccessor]";
    private static final long WAIT_TIME_AFTER_UPLOAD_IN_MILLIS = 15000;
    private static final String SERVER_HOSTNAME = readServerHostname();
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final String HTTP_PORT = "80";
    private static final String HTTPS_PORT = "443";
    private static final String SERVER_HTTP_API_PREFIX = HTTP_PREFIX + SERVER_HOSTNAME + ":" + HTTP_PORT + "/extrasensory/";
    private static final String SERVER_HTTPS_API_PREFIX = HTTPS_PREFIX + SERVER_HOSTNAME + ":" + HTTPS_PORT + "/extrasensory/";

    private static final String FEEDBACK_FILE_EXTENSION = ".feedback";

    private boolean _useHttps = true;
    private SSLContext _sslContext = null;
    private boolean shouldSendWithHttps() {
        return _useHttps && (_sslContext != null);
    }

    private static class ESFeedbackQueue {
        private ArrayList<ESTimestamp> _timestampsQueue;
        private HashMap<ESTimestamp,ESActivity> _activitiesToSend;

        public ESFeedbackQueue() {
            _timestampsQueue = new ArrayList<>(4);
            _activitiesToSend = new HashMap<>(4);
        }

        public void addActivityForFeedback(ESActivity activity) {
            ESTimestamp timestamp = activity.get_timestamp();
            if (!_activitiesToSend.containsKey(timestamp)) {
                _timestampsQueue.add(timestamp);
            }
            _activitiesToSend.put(timestamp,activity);
            // Send notification to other components:
            Intent intent = new Intent(BROADCAST_FEEDBACK_QUEUE_SIZE_CHANGED);
            LocalBroadcastManager.getInstance(ESApplication.getTheAppContext()).sendBroadcast(intent);
        }

        public int size() {
            return _timestampsQueue.size();
        }

        public ESActivity getNextInQueue() {
            if (_timestampsQueue.isEmpty()) {
                return null;
            }
            ESTimestamp timestamp = _timestampsQueue.remove(0);
            _timestampsQueue.add(timestamp);
            return _activitiesToSend.get(timestamp);
        }

        public void removeFromQueue(ESTimestamp timestamp) {
            // Remove from the queue:
            _timestampsQueue.remove(timestamp);
            _activitiesToSend.remove(timestamp);
            // Delete the marking file:
            File feedbackFile = new File(ESApplication.getFeedbackDir(),timestamp.toString() + FEEDBACK_FILE_EXTENSION);
            if (feedbackFile.exists()) {
                feedbackFile.delete();
                Log.i(LOG_TAG,"Deleted feedback file: " + feedbackFile.getName());
            }
            // Send notification to other components:
            Intent intent = new Intent(BROADCAST_FEEDBACK_QUEUE_SIZE_CHANGED);
            LocalBroadcastManager.getInstance(ESApplication.getTheAppContext()).sendBroadcast(intent);
        }

        public String toString() {
            String str = "{";
            if (!_timestampsQueue.isEmpty()) {
                str += _timestampsQueue.get(0);
            }
            for (int i=1;i < _timestampsQueue.size(); i ++) {
                str += "," + _timestampsQueue.get(i);
            }
            str += "}";

            return str;
        }
    }

    private static String readServerHostname() {
        String hostname = "missing.server.hostname";
        Context context = ESApplication.getTheAppContext();
        InputStream inputStream = context.getResources().openRawResource(R.raw.server_hostname);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            hostname = line.trim();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostname;
    }

    private ArrayList<String> _uploadQueue;
    private ESFeedbackQueue _feedbackQueue;
    private long _busyUntilTimeInMillis = 0;
    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(LOG_TAG,"received broadcast with null intent");
                return;
            }
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Log.i(LOG_TAG,"received broadcast of WiFi connectivity change");
                if (canWeUseNetworkNow()) {
                    Log.i(LOG_TAG,"We now have WiFi. Call upload and send from feedback queue");
                    uploadWhatYouHave();
                    sendFeedbackFromQueue();
                }
                else {
                    Log.i(LOG_TAG,"We now don't have WiFi.");
                }
                return;
            }
        }
    };

    /**
     * Singleton implementation:
     */
    private static ESNetworkAccessor _theSingleNetworkAccessor;
    private ESNetworkAccessor() {
        if (_useHttps) {
            prepareTLSContext();
        }
        Log.i(LOG_TAG,"Initializing network accessor. Prepared TLS context: " + _sslContext);

        _uploadQueue = new ArrayList<String>(8);
        _feedbackQueue = new ESFeedbackQueue();
        ESApplication.getTheAppContext().registerReceiver(_broadcastReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        checkZipFilesInDirectory();
        checkFeedbackFilesInDirectory();
    }

    private void prepareTLSContext() {
        // Load CAs from an InputStream
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        InputStream inputStream = ESApplication.getTheAppContext().getResources().openRawResource(R.raw.server_certificate);
        InputStream caInput = new BufferedInputStream(inputStream);
        Certificate ca = null;
        try {
            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            caInput.close();
        } catch (CertificateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Create a KeyStore containing our trusted CAs
        KeyStore keyStore = null;
        try {
            String keyStoreType = KeyStore.getDefaultType();
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
        }
        catch (CertificateException e) {
            e.printStackTrace();
            return;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return;
        }


        // Create an SSLContext that uses our TrustManager
        try {
            _sslContext = SSLContext.getInstance("TLS");
            _sslContext.init(null, tmf.getTrustManagers(), null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return;
        }

    }

    /**
     * Get the network accessor
     * @return The network accessor
     */
    public static ESNetworkAccessor getESNetworkAccessor() {
        if (_theSingleNetworkAccessor == null) {
            _theSingleNetworkAccessor = new ESNetworkAccessor();
        }

        return _theSingleNetworkAccessor;
    }

    public boolean get_useHttps() {
        return _useHttps;
    }

    public void set_useHttps(boolean useHttps) {
        _useHttps = useHttps;
        if (_useHttps && _sslContext == null) {
            prepareTLSContext();
        }
        Log.d(LOG_TAG,"Setting use-https to: " + _useHttps);
    }

    /**
     * Get the upload queue size - how many zip files are waiting to be handled.
     * @return The number of stored examples (zip files)
     */
    public int uploadQueueSize() {
        return _uploadQueue.size();
    }

    /**
     * Get the feedback queue size - how many minute-activities are waiting for their labels to be sent as feedback to the server.
     * @return The number of activities who's labels need to be sent.
     */
    public int feedbackQueueSize() {
        return _feedbackQueue.size();
    }

    private void checkFeedbackFilesInDirectory() {
        File[] feedbackFiles = ESApplication.getFeedbackDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(FEEDBACK_FILE_EXTENSION);
            }
        });

        for (File file : feedbackFiles) {
            String timestampStr = file.getName().replace(FEEDBACK_FILE_EXTENSION,"");
            ESTimestamp timestamp = new ESTimestamp(timestampStr);
            ESActivity activity = ESDatabaseAccessor.getESDatabaseAccessor().getESActivity(timestamp);
            if (activity == null) {
                // Then there is no activity record for this timestamp, and the feedback file should be deleted:
                try {
                    file.delete();
                }
                catch (SecurityException ex) {
                    Log.e(LOG_TAG,"Trouble deleting feedback file: " + file.getName());
                }
                continue;
            }

            // Add the relevant activity to the feedback queue:
            _feedbackQueue.addActivityForFeedback(activity);
        }
        Log.d(LOG_TAG,"Feedback Queue: " + _feedbackQueue);
    }

    private void checkZipFilesInDirectory() {
        File[] zipFiles = ESApplication.getZipDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".zip") || filename.endsWith(".ZIP");
            }
        });

        for (File file : zipFiles) {
            String filename = file.getName();
            addToUploadQueueWithoutUploadingYet(filename);
        }

        uploadWhatYouHave();
    }

    private void addToUploadQueueWithoutUploadingYet(String zipFileName) {
        if (_uploadQueue.contains(zipFileName)) {
            Log.e(LOG_TAG,"Network queue already contains zip file: " + zipFileName);
            return;

        }
        Log.v(LOG_TAG, "Adding to network queue: " + zipFileName);
        _uploadQueue.add(zipFileName);

        // Send notification to other components:
        Intent intent = new Intent(BROADCAST_NETWORK_QUEUE_SIZE_CHANGED);
        LocalBroadcastManager.getInstance(ESApplication.getTheAppContext()).sendBroadcast(intent);
    }

    /**
     *
     * Add a data file (zip) to the queue of examples to upload to the server.
     *
     * @param zipFileName The path of the zip file to upload
     */
    public void addToUploadQueue(String zipFileName) {
        addToUploadQueueWithoutUploadingYet(zipFileName);
        // If we stored enough, we can start uploading sequence:
        if (_uploadQueue.size() > ESSettings.numExamplesStoreBeforeSend()) {
            uploadWhatYouHave();
        }
    }

    private void deleteZipFileAndRemoveFromUploadQueue(String zipFileName) {
        _uploadQueue.remove(zipFileName);
        File file = new File(ESApplication.getZipDir(),zipFileName);
        file.delete();
        Log.i(LOG_TAG,"Deleted and removed from network queue file: " + zipFileName);
        // Send notification to other components:
        Intent intent = new Intent(BROADCAST_NETWORK_QUEUE_SIZE_CHANGED);
        LocalBroadcastManager.getInstance(ESApplication.getTheAppContext()).sendBroadcast(intent);
    }

    public void uploadWhatYouHave() {
        Log.v(LOG_TAG,"uploadWhatYouHave() was called.");
        if (_uploadQueue.size() <= 0) {
            Log.v(LOG_TAG, "Nothing to upload (queue is empty).");
            return;
        }
        // Check if there is WiFi connectivity:
        if (!canWeUseNetworkNow()) {
            Log.i(LOG_TAG,"There is no WiFi right now. Not uploading.");
            return;
        }

        // Check if busy:
        long nowInMillis = new Date().getTime();
        if (nowInMillis < _busyUntilTimeInMillis) {
            Log.i(LOG_TAG,"Network is busy");
            return;
        }
        // Set the "busy until" sign:
        _busyUntilTimeInMillis = nowInMillis + WAIT_TIME_AFTER_UPLOAD_IN_MILLIS;

        // Get the next zip to handle:
        String nextZip = _uploadQueue.remove(0);

        // Keep it at the end of the queue (until getting response):
        _uploadQueue.add(nextZip);

        Log.v(LOG_TAG,"Popped zip from queue: " + nextZip);
        Log.v(LOG_TAG,"Now queue has: " + _uploadQueue);

        // Send the next zip:
        ESApiHandler.ESApiParams params = new ESApiHandler.ESApiParams(
                ESApiHandler.API_TYPE.API_TYPE_UPLOAD_ZIP,nextZip,null,this);
        Log.d(LOG_TAG,"Created api params: " + params);
        ESApiHandler api = new ESApiHandler();
        api.execute(params);
    }

    public boolean canWeUseNetworkNow() {
        if (!ESSettings.isCellularAllowed()) {
            // Then only allowed to use network when having wifi:
            return isThereWiFiConnectivity();
        }

        // Then allowed to use either wifi or cellular:
        return isThereWiFiConnectivity() || isThereMobileConnectivity();
    }

    private boolean isThereMobileConnectivity() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager)ESApplication.getTheAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfoMobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfoMobile.isConnected();
    }

    /**
     * Is the device connected to WiFi?
     * @return
     */
    public boolean isThereWiFiConnectivity() {
        if (ESApplication.debugMode()) {
            Log.v(LOG_TAG,"Debug mode so saying 'wifi available'.");
            return true;
        }
        ConnectivityManager connectivityManager =
                (ConnectivityManager)ESApplication.getTheAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo.isConnected();
    }

    private void createFeedbackFile(ESTimestamp timestamp) {
        try {
            File file = new File(ESApplication.getFeedbackDir(),timestamp.toString() + FEEDBACK_FILE_EXTENSION);
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write(" ");
            output.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Add the activity to the queue of sending feedback to the server.
     * @param activity The activity whose labels we wish to send
     */
    public void addToFeedbackQueue(ESActivity activity) {
        // First, check if the activity still has a zip file waiting to be sent to the server:
        String relevantZipFilename = ESSensorManager.getZipFilename(activity.get_timestamp());
        if (_uploadQueue.indexOf(relevantZipFilename) >= 0) {
            Log.i(LOG_TAG,"Instance " + activity.get_timestamp() + " has zip file waiting to be sent, so not adding feedback file for it now. It should be added later when zip will be sent.");
            return;
        }

        _feedbackQueue.addActivityForFeedback(activity);
        createFeedbackFile(activity.get_timestamp());
        Log.i(LOG_TAG,"Added activity " + activity.get_timestamp() + " to feedback queue, which is now: " + _feedbackQueue);
        sendFeedbackFromQueue();
    }

    private void sendFeedbackFromQueue() {
        Log.v(LOG_TAG,"sendFeedbackFromQueue() was called.");
        if (_feedbackQueue.size() <= 0) {
            Log.v(LOG_TAG, "No labels feedback to send (queue is empty).");
            return;
        }
        // Check if there is WiFi connectivity:
        if (!canWeUseNetworkNow()) {
            Log.i(LOG_TAG,"There is no WiFi right now. Not sending.");
            return;
        }

        // Extract the first item in the queue (and push it to the end, to keep until getting response):
        ESActivity activity = _feedbackQueue.getNextInQueue();
        Log.i(LOG_TAG,"Popped from feedback queue activity: " + activity);
        Log.i(LOG_TAG,"Feedback queue now: " + _feedbackQueue);

        ESApiHandler.ESApiParams params = new ESApiHandler.ESApiParams(ESApiHandler.API_TYPE.API_TYPE_FEEDBACK,null,activity,this);
        Log.d(LOG_TAG,"Created api params: " + params);
        ESApiHandler api = new ESApiHandler();
        api.execute(params);
    }


    private void handleUploadedZip(ESTimestamp timestamp,String zipFilename,String predictedMainActivity,
                                   String[] predictedLabelNames,double[] predictedLabelProbs,
                                   double[] locationLatLong) {
        // Since zip uploaded successfully, can remove it from network queue and delete the file:
        deleteZipFileAndRemoveFromUploadQueue(zipFilename);
        // Update the ESActivity record:
        if (timestamp == null) {
            Log.i(LOG_TAG,"Handling response from upload - for null timestamp");
        }
        else {
            ESDatabaseAccessor dba = ESDatabaseAccessor.getESDatabaseAccessor();
            ESActivity activity = dba.getESActivity(timestamp);
            if (activity == null) {
                Log.e(LOG_TAG,"Response from server refers to non-existing activity record with timestamp: " + timestamp.infoString());
            }
            else {
                predictedMainActivity = adjustPredictedActivity(predictedMainActivity);
                dba.setESActivityServerPrediction(activity, predictedMainActivity,predictedLabelNames,predictedLabelProbs,locationLatLong);
                Log.i(LOG_TAG, "After getting server prediction, activity is now: " + activity);

                // If there is already user labels, send feedback to server:
                if (activity.hasUserProvidedLabels()) {
                    addToFeedbackQueue(activity);
                }
            }
        }

        // Mark network is available:
        markNetworkIsNotBusy();
    }

    private String adjustPredictedActivity(String predictedMainActivity) {
        predictedMainActivity = ESLabelStrings.reverseStandardizeLabelFromNetwork(predictedMainActivity);
        return predictedMainActivity;
    }

    private void markNetworkIsNotBusy() {
        this._busyUntilTimeInMillis = 0;
        uploadWhatYouHave();
    }


    private static class ESApiHandler extends AsyncTask<ESApiHandler.ESApiParams,Void,String> {


        @Override
        protected String doInBackground(ESApiParams... parameters) {
            ESApiParams params = parameters[0];
            doApiRequest(params);
            return null;
        }

        public enum API_TYPE {
            API_TYPE_UPLOAD_ZIP,
            API_TYPE_FEEDBACK
        }

        public static class ESApiParams {
            public API_TYPE _apiType;
            public String _zipFilenameForUpload;
            public ESActivity _activityForFeedback;
            public ESNetworkAccessor _requester;

            public ESApiParams(API_TYPE apiType,String zipFilenameForUpload,ESActivity activity,ESNetworkAccessor requester) {
                _apiType = apiType;
                _zipFilenameForUpload = zipFilenameForUpload;
                _activityForFeedback = activity;
                _requester = requester;
            }

            @Override
            public String toString() {
                return "<apiType: " + _apiType + ", zipFilename: " + _zipFilenameForUpload + ", activity: " + _activityForFeedback + ">";
            }
        }

        private static final int READ_TIMEOUT_MILLIS = 10000;
        private static final int CONNECT_TIMEOUT_MILLIS = 15000;

        private static final String LINE_END = "\r\n";
        private static final String TWO_HYPHENS = "--";
        private static final String BOUNDARY = "0xKhTmLbOuNdArY";

        private static final String REQUEST_FIELD_CLASSIFIER_TYPE = "classifier_type";
        private static final String REQUEST_FIELD_CLASSIFIER_NAME = "classifier_name";
        private static final String REQUEST_FIELD_TIMEZONE_NAME = "timezone_name";

        private static final String RESPONSE_FIELD_TIMESTAMP = "timestamp";
        private static final String RESPONSE_FIELD_SUCCESS = "success";
        private static final String RESPONSE_FIELD_MESSAGE = "msg";
        private static final String RESPONSE_FIELD_ZIP_FILE = "filename";
        private static final String RESPONSE_FIELD_PREDICTED_MAIN_ACTIVITY = "predicted_activity";
        private static final String RESPONSE_FIELD_PREDICTED_LABEL_NAMES = "label_names";
        private static final String RESPONSE_FIELD_PREDICTED_LABEL_PROBS = "label_probs";
        private static final String RESPONSE_FIELD_LOCATION_LAT_LONG = "location_lat_long";

        private static String[] parseJSONArrayOfStrings(JSONArray jsona) {
            try {
                String[] strings = new String[jsona.length()];
                for (int i = 0; i < jsona.length(); i++) {
                    strings[i] = jsona.getString(i);
                }
                return strings;
            }
            catch (org.json.JSONException exception) {
                Log.e(LOG_TAG,"Trouble getting array of strings from JSONArray: " + jsona);
                return new String[]{};
            }
        }

        private static double[] parseJSONArrayOfNumbers(JSONArray jsona) {
            try {
                double[] numbers = new double[jsona.length()];
                for (int i = 0; i < jsona.length(); i++) {
                    numbers[i] = jsona.getDouble(i);
                }
                return numbers;
            }
            catch (org.json.JSONException exception) {
                Log.e(LOG_TAG,"Trouble getting array of doubles from JSONArray: " + jsona);
                return new double[]{};
            }
        }

        private void doApiRequest(ESApiParams params) {
            Log.v(LOG_TAG,"API params: " + params);
            if (params == null) {
                Log.e(LOG_TAG,"Got null params.");
                return;
            }

            switch (params._apiType) {
                case API_TYPE_UPLOAD_ZIP:
                    apiUploadZip(params);
                    break;
                case API_TYPE_FEEDBACK:
                    apiFeedback(params);
                    break;
                default:
                    Log.e(LOG_TAG,"Unsupported api type: " + params._apiType);
            }

        }

        private void prepareConnectionForHTTPS(HttpURLConnection conn,ESApiParams params) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)conn;
            httpsURLConnection.setSSLSocketFactory(params._requester._sslContext.getSocketFactory());
            httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return SERVER_HOSTNAME.equals(hostname);
                }
            });
        }

        private void apiFeedback(ESApiParams params) {
            Resources resources = ESApplication.getTheAppContext().getResources();
            String apiSuffix = resources.getString(R.string.api_feedback) + "?" + prepareFeedbackApiParameters(params._activityForFeedback);
            Log.i(LOG_TAG,"Feedback api call: " + apiSuffix);

            String apiUrl = (params._requester.shouldSendWithHttps() ? SERVER_HTTPS_API_PREFIX : SERVER_HTTP_API_PREFIX)
                    + apiSuffix;
            Log.i(LOG_TAG,"Feedback api url: " + apiUrl);

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (params._requester.shouldSendWithHttps()) {
                    prepareConnectionForHTTPS(conn,params);
                }
                conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                conn.setReadTimeout(READ_TIMEOUT_MILLIS);
                conn.setDoOutput(false);
                conn.setDoInput(true); // Allow Inputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("GET");
                //conn.setRequestProperty("Connection", "Keep-Alive");

                conn.connect();

                JSONObject response = getServerResponseAndDisconnect(conn,"feedback");
                // If we've reached this far, lets remove this activity from the feedback queue:
                params._requester._feedbackQueue.removeFromQueue(params._activityForFeedback.get_timestamp());
                Log.i(LOG_TAG,"Removed " + params._activityForFeedback.get_timestamp() + " from feedback queue. Now: " + params._requester._feedbackQueue);

                // Now lets call to send more feedbacks if the queue isn't empty:
                params._requester.sendFeedbackFromQueue();

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG,"Problem with api URL");
                e.printStackTrace();
            } catch (ProtocolException e) {
                Log.e(LOG_TAG,"Bad HTTP protocol");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(LOG_TAG,"Failed with feedback api");
                e.printStackTrace();
            }
        }

        private JSONObject getServerResponseAndDisconnect(HttpURLConnection conn,String api_type) {
            try {
                // Responses from the server (code and message)
                int responseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i(LOG_TAG, "HTTP Response is : "
                        + serverResponseMessage + ": " + responseCode);

                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String responseStr = stringBuilder.toString();
                Log.i(LOG_TAG, "ESS server responded: " + responseStr);

                conn.disconnect();

                // Analyze the response:
                JSONObject response = new JSONObject(responseStr);
                if (!response.getBoolean(RESPONSE_FIELD_SUCCESS)) {
                    Log.e(LOG_TAG, "Server said "+ api_type + " failed.");
                    Log.e(LOG_TAG, "Server message: " + response.getString(RESPONSE_FIELD_MESSAGE));
                }

                return response;
            }
            catch (IOException e) {
                Log.e(LOG_TAG,"Failed with " + api_type + " api");
                e.printStackTrace();
            }
            catch (JSONException e) {
                Log.e(LOG_TAG,"Failed analyze RMW server response");
                e.printStackTrace();
            }

            return null;
        }

        private String prepareFeedbackApiParameters(ESActivity activity) {
            String uuidStr = "uuid=" + ESSettings.uuid();
            String timestampStr = "timestamp=" + activity.get_timestamp();
            String labelSourceStr = "label_source=" + activity.get_labelSource();
            String mainPredictionStr = "predicted_activity=" + activity.get_mainActivityServerPrediction();
            String mainUserStr = "corrected_activity=" + activity.get_mainActivityUserCorrection();
            String secondaryStr = "secondary_activities=" + ESLabelStrings.makeCSVForNetwork(activity.get_secondaryActivities());
            String moodStr = "moods=" + ESLabelStrings.makeCSVForNetwork(activity.get_moods());

            String apiParams = uuidStr + "&" +
                    timestampStr + "&" +
                    labelSourceStr + "&" +
                    mainPredictionStr + "&" +
                    mainUserStr + "&" +
                    secondaryStr + "&" +
                    moodStr;

            // Adding more information (including about interaction of the user with the app):
            String sendTimeStr = "timestampOfSendingFeedback=" + new ESTimestamp().toString();
            apiParams += "&" + sendTimeStr;
            // Adding information about user interaction with the app:
            String openFeedbackFormTimeStr = "timestampOfOpeningFeedbackForm=" + activity.get_timestampOpenFeedbackForm();
            String sendButtonPressTimeStr = "timestampOfPressingSendFeedbackButton=" + activity.get_timestampPressSendButton();
            String notificationTimeStr = "timestampOfNotificationAppear=" + activity.get_timestampNotification();
            String userResToNotifTimeStr = "timestampOfUserRespondToNotification=" + activity.get_timestampUserRespondToNotification();

            apiParams += "&" + openFeedbackFormTimeStr + "&" + sendButtonPressTimeStr + "&" + notificationTimeStr + "&" + userResToNotifTimeStr;

            apiParams = apiParams.replaceAll(" ","_");
            return apiParams;
        }

        private void apiUploadZip(ESApiParams params) {
            try {
                Resources resources = ESApplication.getTheAppContext().getResources();

                String zipFilename = params._zipFilenameForUpload;
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File zipFile = new File(ESApplication.getZipDir(),zipFilename);
                if (!zipFile.exists()) {
                    Log.e(LOG_TAG,"Zip file doesn't exist: " + zipFilename);
                    return;
                }

                String urlString = (params._requester.shouldSendWithHttps() ? SERVER_HTTPS_API_PREFIX : SERVER_HTTP_API_PREFIX)
                        + resources.getString(R.string.api_upload_zip) +
                        "?" + REQUEST_FIELD_CLASSIFIER_TYPE + "=" + ESSettings.classifierType() +
                        "&" + REQUEST_FIELD_CLASSIFIER_NAME + "=" + ESSettings.classifierName();
                URL url = new URL(urlString);
                Log.i(LOG_TAG,"Api url: " + url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (params._requester.shouldSendWithHttps()) {
                    prepareConnectionForHTTPS(conn,params);
                }
                conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                conn.setReadTimeout(READ_TIMEOUT_MILLIS);
                conn.setDoOutput(true);
                conn.setDoInput(true); // Allow Inputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");

                conn.setRequestProperty(REQUEST_FIELD_CLASSIFIER_TYPE,ESSettings.classifierType());
                conn.setRequestProperty(REQUEST_FIELD_CLASSIFIER_NAME,ESSettings.classifierName());

                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
                conn.setRequestProperty("uploaded_file", zipFilename);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\""
                        + zipFilename + "\"" + LINE_END);

                dos.writeBytes(LINE_END);

                // create a buffer of  maximum size
                FileInputStream fileInputStream = new FileInputStream(zipFile);
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(LINE_END);
                dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END);

                // Send the request:
                conn.connect();

                // Analyse the response:
                ESTimestamp timestamp = null;
                String responseZipFilename = zipFilename;
                String predictedMainActivity = null;
                String[] predictedLabelNames = null;
                double[] predictedLabelProbs = null;
                double[] locationLatLong = null;
                JSONObject response = getServerResponseAndDisconnect(conn,"upload");
                if (response != null) {
                    timestamp = new ESTimestamp(response.getInt(RESPONSE_FIELD_TIMESTAMP));
                    responseZipFilename = response.getString(RESPONSE_FIELD_ZIP_FILE);
                    predictedMainActivity = response.getString(RESPONSE_FIELD_PREDICTED_MAIN_ACTIVITY);

                    predictedLabelNames = ESLabelStrings.reverseStandardizeLabelsFromNetwork(parseJSONArrayOfStrings(response.getJSONArray(RESPONSE_FIELD_PREDICTED_LABEL_NAMES)));
                    predictedLabelProbs = parseJSONArrayOfNumbers(response.getJSONArray(RESPONSE_FIELD_PREDICTED_LABEL_PROBS));
                    if (predictedLabelNames.length != predictedLabelProbs.length) {
                        Log.e(LOG_TAG,"Server responded with prediction label names and label probabilities of inconsistent sizes. Changing them both to empty.");
                    }

                    if (response.isNull(RESPONSE_FIELD_LOCATION_LAT_LONG)) {
                        locationLatLong = null;
                    }
                    else {
                        locationLatLong = parseJSONArrayOfNumbers(response.getJSONArray(RESPONSE_FIELD_LOCATION_LAT_LONG));
                    }
                }

                conn.disconnect();
                params._requester.handleUploadedZip(timestamp, responseZipFilename, predictedMainActivity,
                        predictedLabelNames,predictedLabelProbs,locationLatLong);

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG,"Failed with creating URI for uploading zip");
                e.printStackTrace();
                params._requester.markNetworkIsNotBusy();
            } catch (IOException e) {
                Log.e(LOG_TAG,"Failed with uploading zip");
                e.printStackTrace();
                params._requester.markNetworkIsNotBusy();
            } catch (JSONException e) {
                Log.e(LOG_TAG,"Error parsing the server response");
                e.printStackTrace();
                params._requester.markNetworkIsNotBusy();
            }

        }
    }
}
