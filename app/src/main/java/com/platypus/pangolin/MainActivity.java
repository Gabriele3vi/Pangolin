package com.platypus.pangolin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.samplers.AcousticNoiseSampler;
import com.platypus.pangolin.samplers.Sampler;
import com.platypus.pangolin.samplers.SignalStrengthSampler;
import com.platypus.pangolin.samplers.WifiSampler;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MICROPHONE = 1;
    private Button btn_campiona;
    private Button btn_wifi;
    private Button btn_sample_signal;
    private TextView tv_out;


    private EditText timeToSample;

    final int RECORD_AUDIO_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_campiona = findViewById(R.id.btn_campiona_mic);
        btn_wifi = findViewById(R.id.wifi);
        btn_sample_signal = findViewById(R.id.btn_sample_signal);
        tv_out = findViewById(R.id.tv_out);
        timeToSample = findViewById(R.id.tx_millis);

        //TODO chiedere tutti i permessi all'esecuzione
        askForMicrophonePermissions();

        btn_campiona.setOnClickListener(e -> {
            //startRecording();
            testAcousticNoiseSampler();
        });

        btn_wifi.setOnClickListener(e -> {
            testWifiSampler();
        });

        btn_sample_signal.setOnClickListener(e -> {
            testSignalSampler();

        });
    }

    public void testAcousticNoiseSampler(){
        String millisEditText = timeToSample.getText().toString();
        int millis = !millisEditText.equals("") ? Integer.parseInt(millisEditText) : 1000;
        AcousticNoiseSampler sampler = new AcousticNoiseSampler(millis);

        Sample s = sampler.getSample();
        if (s  == null){
            tv_out.setText("Errore nella lettura del rumore");
        } else {
            tv_out.setText(s.toString());
            System.out.println(s);
        }
    }

    public void testSignalSampler(){
        Sampler signalSampler = new SignalStrengthSampler(this);
        Sample s = signalSampler.getSample();
        if (s  == null){
            tv_out.setText("Errore nella lettura del segnale telefonico");
        } else {
            tv_out.setText(s.toString());
            System.out.println(s);
        }
    }

    public void testWifiSampler(){
        tv_out.setText("Wifiiiiii");
        WifiSampler sampler = new WifiSampler(this);
        Sample s = sampler.getSample();
        if (s  == null){
            tv_out.setText("Errore nella lettura del WIFI");
        } else {
            tv_out.setText(s.toString());
            System.out.println(s);
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

    //TODO scrivere i metodi per chiedere tutti i permessi necessari
    //vedi link di geekforgeeks nella cartella di chrome AndroidDev
    public void askForMicrophonePermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        }
    }

}