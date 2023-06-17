package com.platypus.pangolin.samplers;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.models.SampleType;
import com.platypus.pangolin.models.SignalCondition;

import java.util.ArrayList;
import java.util.List;

public class AcousticNoiseSampler extends Sampler{
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

    private boolean isRecording;

    private Thread recordingThread;
    private Thread timerThread;

    private List<Double> decibelsReadList;
    private int samplingTimeMillis;

    @SuppressLint("MissingPermission")
    public AcousticNoiseSampler(int samplingTimeMillis) {
        this.samplingTimeMillis = samplingTimeMillis >= 500 ? samplingTimeMillis : 1000;
        audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AUDIO_CHANNELS,
                AUDIO_FORMAT,
                BUFFER_SIZE_RECORDING
        );

        decibelsReadList = new ArrayList<>();
    }


    private void sampleNoise() throws IllegalAccessException{
        //check if the audioRecord is initialized
        if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
            throw new IllegalAccessException("AudioRecorder has not been initialized");


        audioRecorder.startRecording();
        isRecording = true;

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
    }

    private void readData(){
        while(isRecording){
            short[] buffer = new short[BUFFER_SIZE_RECORDING];
            int byteRead = audioRecorder.read(buffer, 0, buffer.length);
            double decibelRead = getDecibelFromBuffer(buffer, byteRead);
            if (decibelRead != Double.NEGATIVE_INFINITY) {
                decibelsReadList.add(decibelRead);
            } else {
                System.out.println("beccaato un infinity");
            }
        }
    }

    private double getDecibelFromBuffer(short[] buffer, int byteRead){
        //get the maximum value of the buffer to clean the data
        int max = buffer[0];
        for(int i = 0; i < byteRead; i++) {
            int current = Math.abs(buffer[i]);
            if (current > max)
                max = current;
        }

        //return the max value in DB
        return 20 * Math.log10(max / 32767.0);
    }

    private void stopReading(){
        if(audioRecorder != null) {
            //first, we must stop the reading thread, so we set isRecording to false
            isRecording = false;
            //then we stop the AudioRecorder
            audioRecorder.stop();
            audioRecorder.release();
            recordingThread = null;
            timerThread = null;
        }
    }


    @Override
    public Sample getSample() {
        try {
            sampleNoise();

            double sum = 0;
            for (double db : decibelsReadList)
                sum += db;

            double db = sum / decibelsReadList.size();

            decibelsReadList.clear();

            if (db >= -10)
                return new Sample(SampleType.Noise, SignalCondition.POOR, db);
            else if (db >= -50)
                return new Sample(SampleType.Noise, SignalCondition.GOOD, db);
            else
                return new Sample(SampleType.Noise, SignalCondition.EXCELLENT, db);

        } catch (IllegalAccessException e) {
            System.out.println("erroreeeee");
            return null;
        }
    }
}
