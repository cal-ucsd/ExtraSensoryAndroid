package edu.ucsd.calab.extrasensory.network;

import android.app.IntentService;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESActivity;

/**
 * This class handles the networking with the server.
 * The two main features is uploading a sensor-measurements bundle to the server
 * and sending user label-feedback to the server.
 *
 * This class is designed as a singleton.
 * We need a single network agent to know the current state
 * (e.g. waiting for server response, so not available to upload new examples).
 *
 * Created by Yonatan on 1/17/2015.
 */
public class ESNetworkAccessor {

    private static final String LOG_TAG = "[ESNetworkAccessor]";
    private static final long WAIT_TIME_AFTER_UPLOAD_IN_MILLIS = 15000;

    private ArrayList<String> _networkQueue;
    private long _busyUntilTimeInMillis = 0;

    /**
     * Singleton implementation:
     */
    private static ESNetworkAccessor _theSingleNetworkAccessor;
    private ESNetworkAccessor() {
        _networkQueue = new ArrayList<String>(8);
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

    /**
     * Add a data file (zip) to the queue of examples to upload to the server.
     *
     * @param zipFileName The path of the zip file to upload
     */
    public void addToNetworkQueue(String zipFileName) {
        _networkQueue.add(zipFileName);
        uploadWhatYouHave();
    }

    private void zipFileWasSentSuccessfully(String zipFileName) {
        _networkQueue.remove(zipFileName);
        String filePath = zipFileName;
        File file = new File(filePath);
        file.delete();
        Log.i(LOG_TAG,"Deleted and removed from network queue file: " + zipFileName);
    }

    public void uploadWhatYouHave() {
        if (_networkQueue.size() <= 0) {
            return;
        }
        // Check if there is WiFi connectivity:
        if (!isThereWiFiConnectivity()) {
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
        String nextZip = _networkQueue.remove(0);

        // Keep it at the end of the queue (until getting response):
        _networkQueue.add(nextZip);

        // Send the next zip:
        //todo...........
        //TODO check if busy or not. Get the first in the queue and send it.
    }

    private boolean isThereWiFiConnectivity() {
        return false;
    }

    public void sendFeedback(ESActivity activity) {
        //TODO send the feedback api
    }

    //TODO add the receive response functions............
    //TODO perhaps the receive is handled within the send functions..............


    private class ESApiIntentService extends IntentService {

        /**
         * Creates an IntentService.  Invoked by your subclass's constructor.
         *
         */
        public ESApiIntentService() {
            super("ESApiIntentService");
        }

        public static final String ACTION_UPLOAD_ZIP = "edu.ucsd.calab.extrasensory.api.action.UPLOAD_ZIP";
        public static final String ACTION_FEEDBACK = "edu.ucsd.calab.extrasensory.api.action.FEEDBACK";

        private static final int READ_TIMEOUT_MILLIS = 10000;
        private static final int CONNECT_TIMEOUT_MILLIS = 15000;
        private static final String LINE_END = "\r\n";
        private static final String TWO_HYPHENS = "--";
        private static final String BOUNDARY = "0xKhTmLbOuNdArY";

        @Override
        protected void onHandleIntent(Intent intent) {
            if (intent == null) {
                Log.e(LOG_TAG,"Got null intent.");
                return;
            }

            final String action = intent.getAction();
            if (action == null) {
                Log.e(LOG_TAG,"Got intent with null action.");
                return;
            }

            Log.v(LOG_TAG,"Got intent with action: " + action);
            if (ACTION_UPLOAD_ZIP.equals(action)) {
                apiUploadZip(intent);
            }
        }

        private void apiUploadZip(Intent intent) {
            try {
                Resources resources = ESApplication.getTheAppContext().getResources();

                String zipFilename = ".......";//TODO read from intent
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File zipFile = new File(zipFilename);

                URL url = new URL(resources.getString(R.string.server_api_prefix) + resources.getString(R.string.api_upload_zip));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                conn.setReadTimeout(READ_TIMEOUT_MILLIS);
                conn.setDoOutput(true);
                conn.setDoInput(true); // Allow Inputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
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

                // Responses from the server (code and message)
                int responseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + responseCode);

                // Analyze the response:
                JSONObject response = new JSONObject(serverResponseMessage);

            } catch (MalformedURLException e) {
                Log.e(LOG_TAG,"Failed with creating URI for uploading zip");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(LOG_TAG,"Failed with uploading zip");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(LOG_TAG,"Error parsing the server response");
                e.printStackTrace();
            }

        }
    }
}
