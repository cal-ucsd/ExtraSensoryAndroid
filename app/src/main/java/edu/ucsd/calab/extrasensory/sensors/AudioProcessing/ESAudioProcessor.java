package edu.ucsd.calab.extrasensory.sensors.AudioProcessing;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import edu.ucsd.calab.extrasensory.ESApplication;

/**
 * This class handles the sensing from the microphone:
 * recording audio and processing it to a time series of MFCC features.
 *
 * Created by Yonatan on 4/17/2015.
 */
public class ESAudioProcessor {

    private static final String LOG_TAG = "[ESAudioProcessor]";

    private static final int RECORDER_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int RECORDER_SAMPLING_RATE = 22050;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BUFFER_NUM_ELEMENTS = 4096;
    private static final int BYTES_PER_ELEMENT = 2;
    private static final int RECORDER_BUFFER_SIZE_IN_BYTES = RECORDER_BUFFER_NUM_ELEMENTS * BYTES_PER_ELEMENT;
    private static final int AUDIO_WRITER_BUFFER_SIZE_IN_SHORTS = 1024;
    private static final int AUDIO_FRAME_WINDOW_SIZE = 2048;
    private static final int AUDIO_FRAME_HOP_SIZE = 1024;
    private static final double PREEMPHASIS_COEFFICIENT = 0.97;
    private static final double HAMMING_ALPHA = 0.54;
    private static final double HAMMING_BETTA = 1 - HAMMING_ALPHA;
    private static final int NUM_MEL_FILTERS = 34;
    private static final int NUM_CEPSTRAL_COEFFS = 13;
    private static final boolean USE_FIRST_COEFF = true;

    private static final String SOUND_FILENAME = "sound_16bit_short_values.pcm";
    private static final String MFCC_FILENAME = "sound.mfcc";

    private AudioRecord _audioRecorder = null;
    private Thread _recordingThread = null;
    private boolean _isRecording = false;

    private double[] _hammingWindow;
    private MFCC _mfccProcessor;

    public ESAudioProcessor() {
        // Initialize the hamming window:
        _hammingWindow = new double[AUDIO_FRAME_WINDOW_SIZE];
        double factor = 2*Math.PI / (AUDIO_FRAME_WINDOW_SIZE-1);
        for (int i = 0; i < AUDIO_FRAME_WINDOW_SIZE; i ++) {
            _hammingWindow[i] = HAMMING_ALPHA - HAMMING_BETTA*Math.cos(i*factor);
        }

        // Initialize the MFCC processor:
        _mfccProcessor = new MFCC(NUM_CEPSTRAL_COEFFS,RECORDER_SAMPLING_RATE,NUM_MEL_FILTERS,AUDIO_FRAME_WINDOW_SIZE,false,0,USE_FIRST_COEFF);
    }

    private File getSoundFile() {
        return new File(ESApplication.getDataDir(),SOUND_FILENAME);
    }

    public File getMFCCFile() { return new File(ESApplication.getDataDir(),MFCC_FILENAME); }

    private String getSoundFullFilename() {
        return getSoundFile().getPath();
    }

    public String getMFCCFullFilename() {
        return getMFCCFile().getPath();
    }

    public void clearAudioData() {
        stopRecordingSession(false);

        File soundFile = getSoundFile();
        if (soundFile.exists()) {
            Log.i(LOG_TAG,"clearAudioData: Sound file exists. Deleting it...");
            soundFile.delete();
        }
        else {
            Log.i(LOG_TAG,"clearAudioData: Sound file doesn't exist. Nothing to delete");
        }

        File mfccFile = getMFCCFile();
        if (mfccFile.exists()) {
            Log.i(LOG_TAG,"clearAudioData: MFCC file exists. Deleting it...");
            mfccFile.delete();
        }
        else {
            Log.i(LOG_TAG,"clearAudioData: MFCC file doesn't exist. Nothing to delete");
        }

    }

    public void startRecordingSession() {
        _audioRecorder = new AudioRecord(
                RECORDER_AUDIO_SOURCE,
                RECORDER_SAMPLING_RATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                RECORDER_BUFFER_SIZE_IN_BYTES);
        Log.v(LOG_TAG,"Created new audio recorder");

        _isRecording = true;
        _audioRecorder.startRecording();

        _recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");
        Log.v(LOG_TAG,"Created audio-writing thread");
        _recordingThread.start();
        Log.i(LOG_TAG,"Started audio-writing thread");
    }

    private void writeAudioDataToFile() {
        String filePath = getSoundFullFilename();
        short[] soundFrame = new short[AUDIO_WRITER_BUFFER_SIZE_IN_SHORTS];
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,"Failed to open file output stream for audio data file. " + e.getMessage());
            return;
        }

        DataOutputStream dataOutputStream = null;
        dataOutputStream = new DataOutputStream(fileOutputStream);

        int bufferCount = 0;
        while (_isRecording) {
            bufferCount ++;
            int numShortsRead = _audioRecorder.read(soundFrame,0,AUDIO_WRITER_BUFFER_SIZE_IN_SHORTS);
//            if (bufferCount % 20 == 1) {
//                Log.d(LOG_TAG,String.format("buffer %d) Read %d short values from recorder.",bufferCount,numShortsRead));
//            }

            int short_i;
            for (short_i=0; short_i < numShortsRead; short_i++) {
                try {
                    dataOutputStream.writeShort(soundFrame[short_i]);
                } catch (IOException e) {
                    Log.e(LOG_TAG,String.format("buffer %d) Failed to write short to audio data file. %s",bufferCount,e.getMessage()));
                    break;
                }
            }
            try {
                dataOutputStream.flush();
            } catch (IOException e) {
                Log.e(LOG_TAG,"Failed flushing data from dataOutputStream. " + e.getMessage());
            }
//            if (bufferCount % 20 == 1) {
//                Log.i(LOG_TAG, String.format("buffer %d) Wrote %d short values to audio data file.", bufferCount, short_i));
//            }
        }

        try {
            fileOutputStream.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG,"Failed flushing audio data writing stream. " + e.getMessage());
        }
    }

    public void stopRecordingSession(boolean andCalculateMFCC) {
        _isRecording = false;
        if (_recordingThread != null) {
            _recordingThread.interrupt();
            _recordingThread = null;
        }

        if (_audioRecorder == null) {
            Log.i(LOG_TAG, "There's no audio recording to close");
            return;
        }

        if (_audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            _audioRecorder.stop();
            Log.i(LOG_TAG, "Stopped the audio recording.");
        }
        else {
            Log.i(LOG_TAG,"The audio recorder is not recording, so there's nothing to stop.");
        }

        _audioRecorder.release();
        _audioRecorder = null;

        if (andCalculateMFCC) {
            calculateMFCCFeatures();
        }

    }


    private void calculateMFCCFeatures() {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(getSoundFile());
            bufferedInputStream = new BufferedInputStream(fileInputStream,RECORDER_BUFFER_SIZE_IN_BYTES);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG,"Failed creating input stream to read audio data file. " + e.getMessage());
            return;
        }

        DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
        double[] rawFramePreemphasized = new double[AUDIO_FRAME_WINDOW_SIZE];
        // Read first frame:
        boolean theresMoreData;
        theresMoreData = readIntoFrame(dataInputStream,rawFramePreemphasized,0);
        double[] windowedFrame = new double[AUDIO_FRAME_WINDOW_SIZE];
        windowTheFrame(rawFramePreemphasized,windowedFrame);

        // Prepare the output file:
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(getMFCCFile());
        } catch (IOException e) {
            Log.e(LOG_TAG,"Failed opening MFCC file for writing. " + e.getMessage());
            return;
        }

        // Write the first frame's MFCC features to the file:
        calculateFrameMFCCAndWriteToFile(windowedFrame,fileWriter);

        // Loop over the frames:
        int frameCount = 1;
        int logHop = 20;
        while(theresMoreData) {
            frameCount++;
//            if (frameCount % logHop == 1) {
//                Log.d(LOG_TAG, String.format("frame %d) doing MFCC. Shifting frame...", frameCount));
//            }
            // Hop to next overlapping frame:
            shiftRawFrame(rawFramePreemphasized,AUDIO_FRAME_HOP_SIZE);
//            if (frameCount % logHop == 1) {
//                Log.d(LOG_TAG, String.format("frame %d) doing MFCC. Reading next hop frame...", frameCount));
//            }
            theresMoreData = readIntoFrame(dataInputStream,rawFramePreemphasized,AUDIO_FRAME_HOP_SIZE);
//            if (frameCount % logHop == 1) {
//                Log.d(LOG_TAG, String.format("frame %d) doing MFCC. Windowing...", frameCount));
//            }
            windowTheFrame(rawFramePreemphasized,windowedFrame);
//            if (frameCount % logHop == 1) {
//                Log.d(LOG_TAG, String.format("frame %d) doing MFCC. Calculating the MFCC...", frameCount));
//            }
            calculateFrameMFCCAndWriteToFile(windowedFrame,fileWriter);
//            if (frameCount % logHop == 1) {
//                Log.d(LOG_TAG, String.format("frame %d) doing MFCC. done.", frameCount));
//            }

            // Every few records, flush the writer:
            if (frameCount % 50 == 1) {
                try {
                    fileWriter.flush();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed flushing for writing MFCC file. " + e.getMessage());
                }
            }
        }

        try {
            fileWriter.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG,"Failed last flush for writing MFCC file. " + e.getMessage());
        }

        File mfccFile = getMFCCFile();
        Log.d(LOG_TAG,String.format("Does MFCC file exist: %b. Size: %d",mfccFile.exists(),mfccFile.length()));
    }

    private boolean readIntoFrame(DataInputStream dataInputStream,double[] frameToFill,int startFrom) {
        short shortValue;
        for (int i = startFrom; i < AUDIO_FRAME_WINDOW_SIZE; i ++) {
            try {
                shortValue = dataInputStream.readShort();
            } catch (EOFException e) {
                Log.v(LOG_TAG,"Reached end of audio data file");
                // Fill the rest of the frame with zeros:
                for (int j = i; j < AUDIO_FRAME_WINDOW_SIZE; j ++) {
                    frameToFill[j] = 0;
                }
                // Return value states that there is no more data:
                return false;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed reading short value from audio data file. " + e.getMessage());
                break;
            }
            double doubleVal = (double)shortValue / (double)Short.MAX_VALUE;
            if (PREEMPHASIS_COEFFICIENT > 0 && i>0) {
                // Pre-emphasize. Filter with the previous value:
                doubleVal -= PREEMPHASIS_COEFFICIENT*frameToFill[i-1];
            }
            // Place the pre-emphasized value in the frame array:
            frameToFill[i] = doubleVal;
        }

        // Return value states that there is more data:
        return true;
    }

    private void windowTheFrame(double[] rawFrameIn,double[] windowedFrameOut) {
        for (int i = 0; i < AUDIO_FRAME_WINDOW_SIZE; i ++) {
            windowedFrameOut[i] = _hammingWindow[i] * rawFrameIn[i];
        }
    }

    private void calculateFrameMFCCAndWriteToFile(double[] windowedFrame,FileWriter fileWriter) {
        double[] frameMFCC = _mfccProcessor.getParameters(windowedFrame);
        for (int i = 0; i < frameMFCC.length; i ++) {
            try {
                fileWriter.write("" + frameMFCC[i] + ",");
            } catch (IOException e) {
                Log.e(LOG_TAG,"Failed writing MFCC vector to file. Coefficient " + i + ". " + e.getMessage());
                continue;
            }
        }

        // Add newline:
        try {
            fileWriter.write("\n");
        } catch (IOException e) {
            Log.e(LOG_TAG,"Failed writing newline to MFCC file. " + e.getMessage());
        }

//        try {
//            fileWriter.flush();
//        } catch (IOException e) {
//            Log.e(LOG_TAG,"Failed flushing row of frame MFCC to MFCC file. " + e.getMessage());
//        }
    }

    private void shiftRawFrame(double[] rawFrame,int shiftBy) {
        for (int i = shiftBy; i < AUDIO_FRAME_WINDOW_SIZE; i ++) {
            // Copy the i'th value to shiftBy indices earlier:
            rawFrame[i-shiftBy] = rawFrame[i];
        }
    }
}
