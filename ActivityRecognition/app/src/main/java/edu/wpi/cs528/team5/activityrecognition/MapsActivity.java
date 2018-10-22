package edu.wpi.cs528.team5.activityrecognition;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapsActivity
        extends AppCompatActivity
        implements
        OnMapReadyCallback {

    // TODO revert coodinate to fuller lab
    private static final LatLng fullerLab = new LatLng(42.267626, -71.805755);
    private static final LatLng gordanLibrary = new LatLng(42.274228, -71.806544);

    private int visit_fuller_count = 0;
    private int visit_gordon_count = 0;
    private int visit_fuller_subcount=0;
    private int visit_gordon_subcount=0;
    private String inGeofence="";
    long start = System.currentTimeMillis();

    private static final String TAG = "MapsActivity";
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private GoogleMap mMap;
    private MapView mMapView;
    private Bundle bundle = new Bundle();
    private static final int LOC_PERM_REQ_CODE = 1;
    private static final int GEOFENCE_RADIUS = 35;              //meters
//    private static final int GEOFENCE_EXPIRATION = 6000;        //in milli seconds

    private GeofencingClient geofencingClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    //    private Marker geoFenceMarker;
    private Map<String, Marker> geoFenceMarkerMap;

    private static final String BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST";

    private String TAG1 = MapsActivity.class.getSimpleName();
    BroadcastReceiver broadcastReceiver;
    private int prev_type = DetectedActivity.STILL;
    private String pre_label   = "Still";
    private String label   = "Still";
//    String label = getString(R.string.activity_unknown);



    private TextView txtActivity;
    private ImageView imgActivity;

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.i(TAG,inGeofence);
            switch (message){
                case "fullerLab":
                    inGeofence="Fuller Lab";visit_fuller_subcount=GetStepService().getStep();break;
                case "gordanLibrary":
                    inGeofence="Gordon Library";visit_gordon_subcount=GetStepService().getStep();break;
                case "update":
                    if(inGeofence.isEmpty()) break;
                    if(inGeofence.equals("Fuller Lab")) {
                        Log.i(TAG,"SHOULD UPDATE HERE "+GetStepService().getStep());
                        visit_fuller_subcount=GetStepService().getStep();
                    }
                    else visit_gordon_subcount=GetStepService().getStep();
                    break;
                case "existservice":
                    if(inGeofence.equals("Fuller Lab")) visit_fuller_subcount=0;
                    else visit_gordon_subcount=0;
                    inGeofence="";
                    break;
            }
//            visit_fuller_count += intent.getIntExtra("fuller", 0);
//            visit_gordon_count += intent.getIntExtra("gordon", 0);
            if(visit_fuller_subcount>=6) {visit_fuller_count++;updateVisitGeoFenceTextView(inGeofence);visit_fuller_subcount=0;GetStepService().stopListening();GetStepService().resetStep();inGeofence="";}
            if(visit_gordon_subcount>=6) {visit_gordon_count++;updateVisitGeoFenceTextView(inGeofence);visit_gordon_subcount=0;GetStepService().stopListening();GetStepService().resetStep();inGeofence="";}
            Log.i("visit_fuller_subcount", Integer.toString(visit_fuller_subcount));
            Log.i("visit_gordon_subcount", Integer.toString(visit_gordon_subcount));
//            updateVisitGeoFenceTextView(message);

            Log.d("receiver", "Got message: " + message);
        }
    };

    private void updateVisitGeoFenceTextView(String geoFence) {
        Context context=MapsActivity.this;
        CharSequence text="";
        switch (geoFence) {
            case "Fuller Lab":
                text="You have taken 6 steps inside the Fuller Labs, incrementing counter";
                setTextView(R.id.fuller, visit_fuller_count, getString(R.string.visit_fuller));
                break;
            case "Gordon Library":
                text="You have taken 6 steps inside the Gordon Library, incrementing counter";
                setTextView(R.id.gordon, visit_gordon_count, getString(R.string.visit_gordon));
                break;
                default:
                    break;
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }


    private void setTextView(int textViewId, int count, String string) {
        TextView textView = (TextView)findViewById(textViewId);
        textView.setText(string + Integer.toString(count));
    }
    private String million2(long start){
        long millis = System.currentTimeMillis() - start;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds     = seconds % 60;
        return minutes+" Minutes "+seconds+" Seconds.";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mMapView = findViewById(R.id.map);
        mMapView.getMapAsync(this);
        mMapView.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(BROADCAST_ACTION));

        geofencingClient = LocationServices.getGeofencingClient(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        setTextView(R.id.fuller, 0);
//        setTextView(R.id.gordon, 0);
        bindService(new Intent(this,StepCounterService.class),
                sConnection, BIND_AUTO_CREATE);

        //setContentView(R.layout.activity_main);

        txtActivity = findViewById(R.id.txt_activity);
        //txtConfidence = findViewById(R.id.txt_confidence);
        imgActivity = findViewById(R.id.img_activity);
        //Button btnStartTrcking = findViewById(R.id.btn_start_tracking);
        //Button btnStopTracking = findViewById(R.id.btn_stop_tracking);

//        btnStartTrcking.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startTracking();
//            }
//        });
//
//        btnStopTracking.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                stopTracking();
//            }
//        });

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


        String time;



        Log.e(TAG1, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE) {
            switch (type) {

                case DetectedActivity.RUNNING: {
                    label = getString(R.string.activity_running);
                    imgActivity.setImageResource(R.drawable.running);
                    break;
                }
                case DetectedActivity.STILL: {
                    label = getString(R.string.activity_still);
                    imgActivity.setImageResource(R.drawable.stilling);
                    break;
                }

                case DetectedActivity.WALKING: {
                    label = getString(R.string.activity_walking);
                    imgActivity.setImageResource(R.drawable.walking);
                    break;
                }

            }
            txtActivity.setText(label);
            if(label != pre_label){
                time = "You have " + pre_label + " for " +million2(start);
                Toast.makeText(getApplicationContext(), time, Toast.LENGTH_SHORT).show();
                start = System.currentTimeMillis();
//                prev_type = type;
                pre_label = label;
            }
//            if ( type !=DetectedActivity.UNKNOWN && type!=prev_type) {
//
//                if (!pre_label.equals(getString(R.string.activity_unknown))) {
//                    time = "You have " + pre_label + " for " +million2(start);
//                    Toast.makeText(getApplicationContext(), time, Toast.LENGTH_SHORT).show();
//                    start = System.currentTimeMillis();
//                    prev_type = type;
//                    pre_label = label;
//                }
//
//            }



        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (stepService != null) unbindService(sConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    private void startTracking() {
        Intent intent = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent);
    }

    private void stopTracking() {
        Intent intent = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Add a marker in fuller lab and move the camera
//        LatLng fullerLab = new LatLng(42.275078, -71.806574);
//        LatLng gordanLibrary = new LatLng(42.274228, -71.806544);
        markerForGeofence("fullerLab", fullerLab);
        markerForGeofence("gordanLibrary", gordanLibrary);
        //markerForGeofence(gordanLibrary);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fullerLab, 17.0f));

//        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(LatLng latLng) {
//                removeLocationAlert();
//                markerForGeofence(latLng);
//                  drawGeofence();
//            }
//        });

        showCurrentLocationOnMap();
        startGeofence();
        getLastKnownLocation();
    }

    private void showCurrentLocationOnMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            requestLocationAccessPermission();
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");

        Intent intent = new Intent(this, LocationAlertIntentService.class);
        return PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    private Geofence getGeofence(LatLng latlng) {
        Log.d(TAG, "createGeofence");
        String requestID;
        if(latlng.equals(fullerLab))
            requestID = "fullerLab";
        else
            requestID = "gordanLibrary";

        return new Geofence.Builder()
                .setRequestId(requestID)
                .setCircularRegion(latlng.latitude, latlng.longitude, GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(3000)
                .build();
    }

    // Start Geofence creation process
    private void startGeofence() {
        Log.i(TAG, "startGeofence()");
        if (geoFenceMarkerMap != null && geoFenceMarkerMap.size() != 0) {
            Iterator it = geoFenceMarkerMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                Marker geoFenceMarker = (Marker) pair.getValue();
                Geofence geofence = getGeofence(geoFenceMarker.getPosition());
                addGeofence(geofence, geoFenceMarker);
            }
        }
//        if (geoFenceMarker != null) {
//            Geofence geofence = getGeofence(geoFenceMarker.getPosition());
//            addGeofence(geofence);
//        } else {
//            Log.e(TAG, "Geofence marker is null");
//        }
    }

    private void addGeofence(Geofence geofence, final Marker geoFenceMarker) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            geofencingClient.addGeofences(getGeofencingRequest(geofence), getGeofencePendingIntent())
                    .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.i(TAG, "success");
                            Toast.makeText(MapsActivity.this,
                                    "Location alter has been added",
                                    Toast.LENGTH_SHORT).show();
                            drawGeofence(geoFenceMarker);
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "failure");
                            Toast.makeText(MapsActivity.this,
                                    "Location alter could not be added",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            return;
        } else {
            requestLocationAccessPermission();
        }
    }

    private boolean isLocationAccessPermitted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestLocationAccessPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOC_PERM_REQ_CODE);
    }

    /*private void addLocationAlert(double lat, double lng) {
        String key = "" + lat + "-" + lng;
        Geofence geofence = getGeofence(lat, lng);
        addGeofence(geofence);

    }*/

    private void removeLocationAlert() {
        if (isLocationAccessPermitted()) {
            requestLocationAccessPermission();
        } else {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.i(TAG, "removed successfully");
                                Toast.makeText(MapsActivity.this,
                                        "Location alters have been removed",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.i(TAG, "removed fail");
                                Toast.makeText(MapsActivity.this,
                                        "Location alters could not be removed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle geoFenceLimits;

    private void drawGeofence(Marker geoFenceMarker) {
        Log.d(TAG, "drawGeofence()");

//        if (geoFenceLimits != null)
//            geoFenceLimits.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center(geoFenceMarker.getPosition())
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150))
                .radius(GEOFENCE_RADIUS);
        mMap.addCircle(circleOptions);
    }

    private void markerForGeofence(String location, LatLng latLng) {
        Log.i(TAG, "markerForGeofence(" + latLng + ")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .title(title);
        if (mMap != null) {
            // Remove last geoFenceMarker
//            if (geoFenceMarker != null)
//                geoFenceMarker.remove();
            if (geoFenceMarkerMap == null)
                geoFenceMarkerMap = new HashMap<>();
            if (geoFenceMarkerMap.get(location) == null) {
                geoFenceMarkerMap.put(location, mMap.addMarker(markerOptions));
            }
        }
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Log.i(TAG, location.toString());
                        startLocationUpdates();
                    } else {
                        Log.w(TAG, "No location retrieved yet");
                        startLocationUpdates();
                    }
                }
            });
        } else requestLocationAccessPermission();
    }

    private LocationManager mLocationManager = null;
    private LocationRequest locationRequest;
    private final int UPDATE_INTERVAL = 1000;   // Defined in mili seconds.
    private final int FASTEST_INTERVAL = 900;   // This number in extremely low, and should be used only for debug


    // Start location Updates
    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, getGeofencePendingIntent());
    }

    private static StepCounterService stepService = null;
    private static ServiceConnection sConnection = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepCounterService.StepServiceBinder binder =
                    (StepCounterService.StepServiceBinder) service;
            stepService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            stepService = null;
        }
    };

    public static StepCounterService GetStepService()
    {
        return stepService;
    }
}
