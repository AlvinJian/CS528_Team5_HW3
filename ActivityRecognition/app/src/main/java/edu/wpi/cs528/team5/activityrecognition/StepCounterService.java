package edu.wpi.cs528.team5.activityrecognition;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

public class StepCounterService extends Service implements SensorEventListener {
    static private final String TAG = "StepCounterService";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private float[] gravity;
    private float[] linear_acceleration;
    
    public StepCounterService() {
        gravity = new float[]{0.0f, 0.0f, 0.0f};
        linear_acceleration = new float[] {0.0f, 0.0f, 0.0f};
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        dumpValues();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // print the data in csv format
    private void dumpValues()
    {
        StringBuilder builder = new StringBuilder();

        for (int i=0; i<linear_acceleration.length; ++i)
        {
            builder.append(",");
            builder.append(linear_acceleration[i]);
        }
        builder.append("\n");
        Log.i(TAG, builder.toString());
    }
}
