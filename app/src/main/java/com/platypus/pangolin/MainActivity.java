package com.platypus.pangolin;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.platypus.pangolin.models.Sample;
import com.platypus.pangolin.samplers.AcousticNoiseSampler;
import com.platypus.pangolin.samplers.Sampler;
import com.platypus.pangolin.samplers.SignalStrengthSampler;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btn_campiona;
    private Button btn_stop;
    private Button btn_sample_signal;
    private TextView tv_mic_result;


    private EditText timeToSample;

    final int RECORD_AUDIO_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_campiona = findViewById(R.id.btn_campiona_mic);
        btn_stop = findViewById(R.id.btn_stop_mic);
        btn_sample_signal = findViewById(R.id.btn_sample_signal);
        tv_mic_result = findViewById(R.id.tv_microphone_value);
        timeToSample = findViewById(R.id.tx_millis);

        btn_campiona.setOnClickListener(e -> {
            //startRecording();
            testAcousticNoiseSampler();
        });

        btn_stop.setOnClickListener(e -> {
            Toast.makeText(this, "stop", Toast.LENGTH_LONG).show();
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
        System.out.println(s);
    }

    public void testSignalSampler(){
        Sampler signalSampler = new SignalStrengthSampler(this);
        Sample s = signalSampler.getSample();
        System.out.println(s.toString());
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