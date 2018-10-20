package edu.wpi.cs528.team5.activityrecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    private static int count = 0;
    private static long total;
    private int prev_type = DetectedActivity.STILL;
    private String pre_label;



    private TextView txtActivity, txtConfidence;
    private ImageView imgActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtActivity = findViewById(R.id.txt_activity);
        txtConfidence = findViewById(R.id.txt_confidence);
        imgActivity = findViewById(R.id.img_activity);
        Button btnStartTrcking = findViewById(R.id.btn_start_tracking);
        Button btnStopTracking = findViewById(R.id.btn_stop_tracking);

        btnStartTrcking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        btnStopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };

        startTracking();
    }

    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.activity_unknown);

        String time;

        int minute;
        int second;

        switch (type) {
//            case DetectedActivity.IN_VEHICLE: {
//                label = getString(R.string.activity_in_vehicle);
//                break;
//            }
//            case DetectedActivity.ON_BICYCLE: {
//                label = getString(R.string.activity_on_bicycle);
//                break;
//            }
//            case DetectedActivity.ON_FOOT: {
//                label = getString(R.string.activity_on_foot);
//                break;
//            }
            case DetectedActivity.RUNNING: {
                label = getString(R.string.activity_running);
                //imgActivity.setImageResource(R.drawable.running);
                break;
            }
            case DetectedActivity.STILL: {
                label = getString(R.string.activity_still);
                //imgActivity.setImageResource(R.drawable.stilling);
                break;
            }
//            case DetectedActivity.TILTING: {
//                label = getString(R.string.activity_tilting);
//                break;
//            }
            case DetectedActivity.WALKING: {
                label = getString(R.string.activity_walking);
                //imgActivity.setImageResource(R.drawable.walking);
                break;
            }
//            case DetectedActivity.UNKNOWN: {
//                label = getString(R.string.activity_unknown);
//                break;
//            }
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE) {
            //pre_label = label;
            txtActivity.setText(label);
            if (label.equals(getString(R.string.activity_running))) {
                imgActivity.setImageResource(R.drawable.running);
            }
            if (label.equals(getString(R.string.activity_still))) {
                imgActivity.setImageResource(R.drawable.stilling);
            }
            if (label.equals(getString(R.string.activity_walking))) {
                imgActivity.setImageResource(R.drawable.walking);
            }
            if (prev_type == type) {
                count += 1;
            } else {
                //count += 1;
                total = count * Constants.DETECTION_INTERVAL_IN_MILLISECONDS;
                minute = (int) total / 60000;
                second = (int) (total - minute * 60000) / 1000;
                time = "You have " + pre_label + " for " + minute + " min " + second + " seconds ";
                if (!pre_label.equals(getString(R.string.activity_unknown))) {
                    Toast.makeText(getApplicationContext(), time, Toast.LENGTH_SHORT).show();
                }

            }
            prev_type = type;
            pre_label = label;



            //txtConfidence.setText("Confidence: " + confidence);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void startTracking() {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent);
    }

    private void stopTracking() {
        Intent intent = new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }
}