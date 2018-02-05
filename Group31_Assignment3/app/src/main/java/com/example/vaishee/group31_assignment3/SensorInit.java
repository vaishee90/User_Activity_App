package com.example.vaishee.group31_assignment3;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.vaishee.group31_assignment3.MainActivity.db;

/**
 * Created by vaishee on 3/4/17.
 */

public class SensorInit extends Service implements SensorEventListener {

    static int counter = 1, sampler = 1;
    SensorManager sensorManager;
    Cursor dbCursor;
    Sensor accelerometer;
    static StringBuilder insertSql;
    int id;
    static String activity_label, table_name;
    Intent intent_Here;
    Bundle getBundle = null;
    public void onCreate(){
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, 100000);
        insertSql = new StringBuilder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        intent_Here = intent;
        if(intent != null)
            getBundle = intent.getExtras();
        return START_STICKY;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(sampler <= 30)
        {
            if(counter <= 50)
            {
                /*if(counter == 10)
                    Toast.makeText(this, "1 sec over", Toast.LENGTH_SHORT).show();
                if(counter == 20)
                    Toast.makeText(this, "2 sec over", Toast.LENGTH_SHORT).show();
                if(counter == 30)
                    Toast.makeText(this, "3 sec over", Toast.LENGTH_SHORT).show();
                if(counter == 40)
                    Toast.makeText(this, "4 sec over", Toast.LENGTH_SHORT).show();
                if(counter == 50)
                    Toast.makeText(this, "5 sec over", Toast.LENGTH_SHORT).show();*/
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
                Toast.makeText(this, sampler + " sample(s) collected", Toast.LENGTH_SHORT).show();

                counter = 1;
                insertSql.delete(0,insertSql.length());
                insertSql = new StringBuilder();

                sampler++;
                //id++;
            }
        }
        else
        {
            Toast.makeText(this, "sample collection for " + activity_label + " activity is complete", Toast.LENGTH_SHORT).show();

            stopService(intent_Here);
            sensorManager.unregisterListener(this);
            sampler = 1;
        }
    }
}
