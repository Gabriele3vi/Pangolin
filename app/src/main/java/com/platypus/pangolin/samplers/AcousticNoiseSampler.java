package com.platypus.pangolin.samplers;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;

import android.annotation.SuppressLint;
import android.app.ZygotePreload;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.dynamicanimation.animation.SpringAnimation;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.models.SignalCondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AcousticNoiseSampler extends Sampler {
    private AudioRecord audioRecorder;
    private final int SAMPLE_RATE = 16000;
    private final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int RAW_AUDIO_SOURCE = MediaRecorder.AudioSource.UNPROCESSED;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AUDIO_CHANNELS,
            AUDIO_FORMAT
    );

    private final AtomicBoolean isRecording = new AtomicBoolean(false);

    private Thread recordingThread;
    private Thread timerThread;

    private List<Double> decibelsReadList;
    private double decibelsRead;
    private int samplingTimeMillis;

    @SuppressLint("MissingPermission")
    public AcousticNoiseSampler(int samplingTimeMillis) {
        this.samplingTimeMillis = samplingTimeMillis >= 500 ? samplingTimeMillis : 1000;
        decibelsReadList = new ArrayList<>();
        decibelsRead = 0;
    }


    private synchronized void sampleNoise() throws IllegalAccessException{
        initializeSampler();
        //check if the audioRecord is initialized
        if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
            throw new IllegalAccessException("AudioRecorder has not been initialized");

        audioRecorder.startRecording();

        isRecording.set(true);

        //create and start the thread to read the data from the mic

        recordingThread = new Thread(this::readData);
        recordingThread.start();

        //create and start the timer thread
        timerThread = new Thread(() -> {
            try {
                Thread.sleep(samplingTimeMillis);
                stopReading();
                System.err.println("Stopped sampling!");
            } catch (InterruptedException e) {
                stopReading();
                System.err.println("Timer error!");
            }
        });

        timerThread.start();

        try {
            recordingThread.join();
        } catch (InterruptedException e) {
            System.err.println("error during joining the thread");
        }
        //readData2();
        //stopReading();
    }

    private void readData(){
        while(isRecording.get() && audioRecorder != null){
            short[] buffer = new short[BUFFER_SIZE_RECORDING];
            int byteRead = audioRecorder.read(buffer, 0, buffer.length);
            System.out.println(Arrays.toString(buffer));
            double db = getDecibelFromBuffer(buffer, byteRead);
            if (db != Double.NEGATIVE_INFINITY) {
                decibelsReadList.add(db);
                System.out.println(db);
            } else {
                System.out.println("beccato un infinity");
            }
        }
    }

    private void readData2(){
        short[] buffer = new short[BUFFER_SIZE_RECORDING];
        int byteRead = audioRecorder.read(buffer, 0, buffer.length);
        double db = getDecibelFromBuffer(buffer, byteRead);
        if (db != Double.NEGATIVE_INFINITY) {
            decibelsRead = db;
        } else {
            System.out.println("beccato un infinity");
        }
    }

    private double getDecibelFromBuffer(short[] buffer, int byteRead){
        //get the maximum value of the buffer to clean the data
        int countedValues = 0;
        float mean = 0;
        for (int i : buffer){
            int w = Math.abs(i);
            if (w != 0){
                mean += w;
                countedValues ++;
            }
        }
        //return the max value in DB
        mean = mean / countedValues;
        System.out.println(mean);
        return 20 * Math.log10(mean / 32767.0);
    }

    private void stopReading(){
        if(audioRecorder != null) {
            //first, we must stop the reading thread, so we set isRecording to false
            isRecording.set(false);
            //then we stop the AudioRecorder
            audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
            recordingThread = null;
            timerThread = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeSampler(){
        isRecording.set(true);
        audioRecorder = new AudioRecord (
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AUDIO_CHANNELS,
                AUDIO_FORMAT,
                BUFFER_SIZE_RECORDING
        );
    }
    @Override
    public Sample getSample() {
        try {
            sampleNoise();

            double mean = 0;

            for (double f : decibelsReadList) {
                System.out.println("Value stored: " + f);
                mean += f;
            }

            double db = mean / decibelsReadList.size();

            if (db >= -20)
                return new Sample(SampleType.Noise, SignalCondition.POOR, db);
            else if (db >= -40)
                return new Sample(SampleType.Noise, SignalCondition.GOOD, db);
            else
                return new Sample(SampleType.Noise, SignalCondition.EXCELLENT, db);

        } catch (IllegalAccessException e) {
            System.out.println("erroreeeee");
            return null;
        }
    }
}
