package com.platypus.pangolin.activities;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.platypus.pangolin.R;
import com.platypus.pangolin.database.DatabaseHelper;
import com.platypus.pangolin.databinding.ActivityExportDataBinding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExportDataActivity extends DrawerBaseActivity {

    ActivityExportDataBinding activityExportDataBinding;
    EditText fileNameET;
    Spinner sampleTypeSpinner, sampleLocationSpinner;
    Button btn_exportData;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityExportDataBinding = ActivityExportDataBinding.inflate(getLayoutInflater());
        setContentView(activityExportDataBinding.getRoot());
        setActivityTitle("Export data");

        fileNameET = findViewById(R.id.exportFileNameEditText);
        sampleLocationSpinner = findViewById(R.id.spinnerSampleFrom);
        sampleTypeSpinner = findViewById(R.id.spinnerSampleType);
        btn_exportData = findViewById(R.id.btn_export);

        setUpSpinners();

        btn_exportData.setOnClickListener(e -> exportData());
        db = new DatabaseHelper(this);
  
    }

    private void setUpSpinners(){
        //set spinners data
        ArrayAdapter<CharSequence> spinnerLocationAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.sample_from,
                android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        spinnerLocationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        sampleLocationSpinner.setAdapter(spinnerLocationAdapter);


        ArrayAdapter<CharSequence> sampleTypeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.sample_types_all,
                android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        sampleTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        sampleTypeSpinner.setAdapter(sampleTypeAdapter);
    }

    private void exportData() {
        String fileName = fileNameET.getText().toString().trim().toLowerCase();
        String sampleTypeChoice = sampleTypeSpinner.getSelectedItem().toString();
        String sampleOriginChoice = sampleLocationSpinner.getSelectedItem().toString();

        if (fileName.equals("")) {
            Toast.makeText(this, "File must have a name", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> CSVLines = getCSVLines(sampleTypeChoice, sampleOriginChoice);

        //process to generate and export the file
        try {
            File dump = new File(this.getFilesDir(),fileName + ".csv");

            // Create a File object for the file
            FileWriter fw = new FileWriter(dump);

            for (String line : CSVLines)
                fw.write(line);

            fw.close();

            Uri test = FileProvider.getUriForFile(this, "com.example.myapp.fileprovider", dump);

            System.out.println(test);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");

            intent.putExtra(Intent.EXTRA_STREAM, test);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Dump");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "Send samples");
            startActivity(chooser);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            Toast.makeText(this, "Error while exporting file", Toast.LENGTH_SHORT).show();
        }
    }

    private String fromArrayToCSVLine(String [] strings){
        String line = "";

        for(int i = 0; i < strings.length - 1; i++){
            line += strings[i] + ";";
        }

        line += strings[strings.length-1] + "\n";

        return line;
    }

    private List<String> getCSVLines(String sampleType, String origin){
        ArrayList<String> lines = new ArrayList<>();

        Cursor query = db.getData(origin, sampleType);

        String firstLine = fromArrayToCSVLine(query.getColumnNames());
        lines.add(firstLine);
        while(query.moveToNext()) {
            String [] data = new String[query.getColumnCount()];
            for (int i = 0; i < query.getColumnCount(); i++){
                data[i] = query.getString(i);
            }
            String line = fromArrayToCSVLine(data);
            lines.add(line);
        }
        return lines;
    }
}