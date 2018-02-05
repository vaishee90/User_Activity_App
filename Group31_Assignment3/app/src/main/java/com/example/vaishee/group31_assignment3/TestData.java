package com.example.vaishee.group31_assignment3;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;

import static com.example.vaishee.group31_assignment3.MainActivity.db;
import static com.example.vaishee.group31_assignment3.SensorInit.insertSql;

/**
 * Created by vaishee on 10/4/17.
 */

public class TestData extends Service implements SensorEventListener {

    static int counter = 1;
    SensorManager sensorManager;
    Cursor dbCursor;
    Sensor accelerometer;
    static StringBuilder insertSql;
    int id;
    static String activity_label, table_name;
    Intent intent_Here;
    Bundle getBundle = null;
    FileOutputStream fout, fout_test;
    BufferedWriter bw, bw_test;
    File file, file_test;

    public TestData() throws FileNotFoundException {

    }

    public void onCreate () {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, 100000);
        insertSql = new StringBuilder();

        //packagename = "com.example.vaishee.group31_assignment3";

        try {
            file = new File("/data/data/" + getPackageName() + "/training_data.txt");
            file_test = new File("/data/data/" + getPackageName() + "/test_data.txt");
            fout = new FileOutputStream(file);
            fout_test = new FileOutputStream(file_test);
            bw = new BufferedWriter(new OutputStreamWriter(fout));
            bw_test = new BufferedWriter(new OutputStreamWriter(fout_test));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Nullable
    @Override
    public IBinder onBind (Intent intent){
        return null;
    }

    @Override
    public int onStartCommand (Intent intent,int flags, int startId){
        intent_Here = intent;
        if(intent != null)
            getBundle = intent.getExtras();
        return START_STICKY;
    }

    @Override
    public void onAccuracyChanged (Sensor sensor,int accuracy){

    }

    @Override
    public void onSensorChanged  (SensorEvent event){
        if(counter <= 50)
        {
            if(counter == 10)
                Toast.makeText(this, "1 sec over", Toast.LENGTH_SHORT).show();
            if(counter == 20)
                Toast.makeText(this, "2 sec over", Toast.LENGTH_SHORT).show();
            if(counter == 30)
                Toast.makeText(this, "3 sec over", Toast.LENGTH_SHORT).show();
            if(counter == 40)
                Toast.makeText(this, "4 sec over", Toast.LENGTH_SHORT).show();
            if(counter == 50)
                Toast.makeText(this, "5 sec over", Toast.LENGTH_SHORT).show();
            insertSql.append(event.values[0]);
            insertSql.append(",");
            insertSql.append(event.values[1]);
            insertSql.append(",");
            insertSql.append(event.values[2]);
            if(counter < 50)
                insertSql.append(",");

            counter++;
        }
        else
        {
            id = getBundle.getInt("index");
            activity_label = getBundle.getString("label");
            table_name = getBundle.getString("table_name");

            db.execSQL("insert into " + table_name + " values " + "(" + id + "," + insertSql + ", '" + activity_label + "')");
            Toast.makeText(this, "sample collected", Toast.LENGTH_SHORT).show();

            counter = 1;
            insertSql.delete(0,insertSql.length());
            insertSql = new StringBuilder();

            dbCursor = db.rawQuery("select * from " + table_name, null);

            dbCursor.moveToFirst();

            Toast.makeText(this, "writing into file...", Toast.LENGTH_SHORT).show();
            do {
                if(!dbCursor.getString(0).equals("4"))
                {
                    String output = "+" + dbCursor.getString(0);
                    for(int i = 1; i <= 150; i++)
                        output = output + " " + dbCursor.getString(i);
                    System.out.println(output);
                    try {
                        bw.write(output);
                        bw.newLine();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    String output = "+" + dbCursor.getString(0);
                    for(int i = 1; i <= 150; i++)
                        output = output + " " + dbCursor.getString(i);
                    System.out.println(output);
                    try {
                        bw_test.write(output);
                        bw_test.newLine();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }while (dbCursor.moveToNext());

            try
            {
                bw.close();
                bw_test.close();
            }
            catch (Exception e)
            {

            }
            dbCursor.close();

            stopService(intent_Here);
            sensorManager.unregisterListener(this);
        }
    }
}