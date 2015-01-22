package edu.ucsd.calab.extrasensory.network;

import java.util.ArrayList;

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

    private ArrayList<String> _networkQueue;

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

    public void uploadWhatYouHave() {
        if (_networkQueue.size() <= 0) {
            return;
        }
        //TODO check if busy or not. Get the first in the queue and send it.
    }

    public void sendFeedback(ESActivity activity) {
        //TODO send the feedback api
    }

    //TODO add the receive response functions............
}
