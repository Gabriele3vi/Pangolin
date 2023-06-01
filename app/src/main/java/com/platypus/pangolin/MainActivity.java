package com.platypus.pangolin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private Button btn_campiona;
    private Button btn_stop;
    private TextView tv_mic_result;

    private EditText timeToSample;

    final int RECORD_AUDIO_PERMISSION_CODE = 1;
    private AudioRecord recorder;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_campiona = findViewById(R.id.btn_campiona_mic);
        btn_stop = findViewById(R.id.btn_stop_mic);
        tv_mic_result = findViewById(R.id.tv_microphone_value);
        timeToSample = findViewById(R.id.tx_millis);

        isRecording = false;

        btn_campiona.setOnClickListener(e -> {
            //startRecording();
            testAcousticNoiseSampler();
        });

        btn_stop.setEnabled(false);
        btn_stop.setOnClickListener(e -> {
            stopRecording();
        });
    }

    public void testAcousticNoiseSampler(){
        int millis = Integer.parseInt(timeToSample.getText().toString());
        AcousticNoiseSampler sampler = new AcousticNoiseSampler(millis);
        try {
            double dbSampled = sampler.sample();
            tv_mic_result.setText(dbSampled + "");
        } catch (IllegalAccessException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void startRecording() {
        //check if the permissions for the mic are given, if not, ask for them.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE
            );
        }
        //TODO handle when permissions are not given

        //create the recorder
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AUDIO_CHANNELS,
                AUDIO_FORMAT,
                BUFFER_SIZE_RECORDING
        );

        //check if the object has been initialized
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Toast.makeText(this, "Errore", Toast.LENGTH_LONG).show();
            return;
        }

        //now we can start recording

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(() -> {
            readData();
        });
        recordingThread.start();
        btn_stop.setEnabled(true);
    }

    private void readData(){
        while(isRecording) {
            //this array contains the data read from the MIC
            short[] buffer = new short[BUFFER_SIZE_RECORDING];
            //recorder.read() fill the array with the data and returns the arrays' length
            int byteRead = recorder.read(buffer, 0, buffer.length);
            double [] db = shortToDB(buffer, byteRead);
            tv_mic_result.setText(db[0] + " ");
            //System.out.println("max = " + db[0]);
            System.out.println("RMS = " + db[1]);

        }
    }

    private double [] shortToDB(short[] buffer, int byteRead){
        short p2 = (buffer[buffer.length-1]);
        double[] mes = new double[2];
        //prendo il max dei valori nel buffer
        long sum = 0;
        int max = buffer[0];

        for(int i = 0; i < byteRead; i++) {
            int current = Math.abs(buffer[i]);
            if (current > max)
                max = current;

            sum += current*current;
        }

        double RMS = Math.sqrt(sum) / byteRead;

        //ci faccio il valore assoluto e applico la formula
        mes[0] = 20 * Math.log10(max / 32767.0);
        mes[1] = 20 * Math.log10((RMS) / 32767.0);
        return mes;
    }


    public void stopRecording(){
        if(recorder != null) {
            //first, we must stop the reading thread, so we set isRecording to false
            isRecording = false;
            //then we stop the AudioRecorder
            recorder.stop();
            recorder.release();
            recordingThread = null;
            btn_stop.setEnabled(false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Autorizzazione concessa", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Autorizzazione non concessa", Toast.LENGTH_SHORT).show();
            }
        }
    }
}