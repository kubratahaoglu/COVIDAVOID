package com.example.covidavoid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;//for overlay
import com.google.android.gms.maps.model.BitmapDescriptorFactory;//for overlay
import com.google.android.gms.maps.model.GroundOverlayOptions;//for overlay
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final int req_interval = 10000;
    public static final int fastest_req_interval = 5000;
    public static final float width = 1657600f;//1657600
    public static final float height = 700100f;//700100
    public static final double overlay_lat = 38.9595;//38.9595
    public static final double overlay_lng = 35.2495;//35.2495
    public static final double east_bnd = 25.9536;
    public static final double north_bnd = 42.1047;
    public static final double alpha = 264.4120;//fact
    public static final double gamma = 335.5185;//341.9745//340.0045
    public int i=0;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    LocationCallback locationCallback;
    public String CHANNEL_ID = "channel1";
    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "ahn4nxt61q2nf-ats.iot.us-east-2.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-2:767567d1-9af0-48af-8d45-ece9fd248ca0";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_2;

    AWSIotMqttManager mqttManager;
    String clientId;

    CognitoCachingCredentialsProvider credentialsProvider;

    Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        client = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                updateLocValues(location);
            }
        };

        if (ActivityCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
        clientId = UUID.randomUUID().toString();


        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnConnect.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);

            }
        }
    };


    private void getCurrentLocation() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(req_interval);
        locationRequest.setFastestInterval(fastest_req_interval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        client.requestLocationUpdates(locationRequest, locationCallback, null);
        client.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                updateLocValues(location);
            }
        });

    }

    private void updateLocValues(final Location location) {
        if (location != null) {
            supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    //googleMap.clear();// clears whole map
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    MarkerOptions marker = new MarkerOptions();
                    marker.position(latLng);
                    marker.title("I'm here.");
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    googleMap.setMyLocationEnabled(true);

                    LatLng origin = new LatLng(overlay_lat, overlay_lng);
                    GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions();
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.tc);
                    BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                    groundOverlayOptions.image(bitmapDescriptor);

                    //GROUND OVERLAYING
                    if(i == 0) {
                        groundOverlayOptions.position(origin, width, height);
                        googleMap.addGroundOverlay(groundOverlayOptions);
                        i++;
                    }

                    int[] pixel = loctoPix(latLng);
                    int x = pixel[0];
                    int y = pixel[1];
                    int pixel1 = bitmap.getPixel(x,y);
                    int r = Color.red(pixel1);
                    int g = Color.green(pixel1);
                    int b = Color.blue(pixel1);
                    String msg;
                    final String topic = "hello";
                    if(r>g && r>b) {
                        Toast.makeText(MainActivity.this, "You're in the danger zone!", Toast.LENGTH_LONG).show();
                        ShowNotification();
                        msg = "Latitude: " + latLng.latitude + "\nLongitude: " + latLng.longitude + "\nDanger Zone!";
                    }
                    else{
                        Toast.makeText(MainActivity.this, "You're in the safe zone.", Toast.LENGTH_LONG).show();
                        msg = "Latitude: " + latLng.latitude + "\nLongitude: " + latLng.longitude + "\nSafe Zone!";

                    }
                    try {
                        mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Publish error.", e);
                    }
                }
            });
        }
    }

    private void ShowNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Danger Zone Alert!")
                .setContentText("You are in a high density corona virus region!");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, builder.build());
    }

    public int[] loctoPix(LatLng latLng){
        int[] arr = new int[2];
        int harman;
        arr[0] = (int) (((latLng.longitude-east_bnd)* alpha)+76);
        if(latLng.latitude > 39.8408){
            harman = (int) (599.6198-(latLng.latitude*14.1970));
            arr[1] = (int) (((north_bnd-latLng.latitude)* gamma)+harman);
        }else{
            arr[1] = (int) (((north_bnd-latLng.latitude)* gamma)+34);
        }
        //int pixel = bitmap.getPixel(0,0);
        return arr;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 44){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation();
            }
        }
    }
}
