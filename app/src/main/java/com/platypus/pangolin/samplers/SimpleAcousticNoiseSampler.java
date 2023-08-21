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
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleAcousticNoiseSampler extends Sampler {

    private AudioRecord audioRecorder;
    private final int SAMPLE_RATE = 16000;
    private final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE_RECORDING = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AUDIO_CHANNELS,
            AUDIO_FORMAT
    );
    private double decibelsRead;

    @SuppressLint("MissingPermission")
    public SimpleAcousticNoiseSampler() {
        decibelsRead = 0;
        System.out.println("Audiorecorder initialized");
    }

    private double getDecibelFromBuffer(short[] buffer, int byteRead){
        //get the maximum value of the buffer to clean the data
        int mean = 0;
        for (int i : buffer){
            int w = Math.abs(i);
            mean += w;
        }
        mean = mean / buffer.length;
        //return the max value in DB
        return 20 * Math.log10(mean / 32767.0);
    }

    @SuppressLint("MissingPermission")
    @Override
    public Sample getSample() {
        audioRecorder = new AudioRecord (
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AUDIO_CHANNELS,
                AUDIO_FORMAT,
                BUFFER_SIZE_RECORDING
        );

        short[] buffer = new short[BUFFER_SIZE_RECORDING];
        int byteRead = audioRecorder.read(buffer, 0, buffer.length);

        //stop the recorder
        audioRecorder.stop();
        audioRecorder.release();
        audioRecorder = null;

        decibelsRead = getDecibelFromBuffer(buffer, byteRead);

        if (decibelsRead >= -20)
            return new Sample(SampleType.Noise, SignalCondition.POOR, decibelsRead);
        else if (decibelsRead >= -40)
            return new Sample(SampleType.Noise, SignalCondition.GOOD, decibelsRead);
        else
            return new Sample(SampleType.Noise, SignalCondition.EXCELLENT, decibelsRead);
    }
}
