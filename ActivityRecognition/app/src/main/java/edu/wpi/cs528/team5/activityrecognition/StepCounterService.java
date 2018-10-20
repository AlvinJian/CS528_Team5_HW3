package edu.wpi.cs528.team5.activityrecognition;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class StepCounterService extends Service
        implements SensorEventListener, StepDetector.StepListener {
    static private final String TAG = "StepCounterService";

    private SensorManager sensorManager;
    private Sensor accel;
    private StepDetector simpleStepDetector;
    private HandlerThread sensorThread;
    private Handler sensorHandler;
    private IBinder mBinder = new StepServiceBinder();
    private int step;
    
    public StepCounterService() {
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        sensorThread = new HandlerThread("sensor");
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());
        step = 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        sensorThread.quitSafely();
    }

    public void startListening()
    {
        sensorManager.registerListener(this, accel,
                SensorManager.SENSOR_DELAY_FASTEST, sensorHandler);
        Toast.makeText(this, "start listening sensor" ,
                Toast.LENGTH_SHORT).show();
    }

    public void stopListening()
    {
        sensorManager.unregisterListener(this);
        Toast.makeText(this, "stop listening sensor" ,
                Toast.LENGTH_SHORT).show();
    }

    public void resetStep()
    {
        synchronized (this) {
            step = 0;
        }
    }

    public int getStep()
    {
        int ret = 0;
        synchronized (this) {
            ret = step;
        }
        return ret;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void step(long timeNs) {
        synchronized (this)
        {
            ++step;
            Log.d(TAG, "current steps: "+ step);
        }
    }

    public class StepServiceBinder extends Binder
    {
        public StepCounterService getService()
        {
            return StepCounterService.this;
        }
    }
}
