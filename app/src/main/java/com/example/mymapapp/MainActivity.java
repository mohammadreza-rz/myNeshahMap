package com.example.mymapapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.layers.VectorElementEventListener;
import org.neshan.layers.VectorElementLayer;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.AnimationStyle;
import org.neshan.styles.AnimationStyleBuilder;
import org.neshan.styles.AnimationType;
import org.neshan.styles.MarkerStyle;
import org.neshan.styles.MarkerStyleCreator;
import org.neshan.ui.ClickData;
import org.neshan.ui.ClickType;
import org.neshan.ui.ElementClickData;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;
import org.neshan.utils.BitmapUtils;
import org.neshan.vectorelements.Marker;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final int BASE_MAP_INDEX = 0;
    MapView map;
    VectorElementLayer markerLayer;
    private ArrayList<LngLat> markerPoss;
    final int REQUEST_CODE = 123;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private Location userLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private String lastUpdateTime;
    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;
    private Marker lastLOc = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // everything related to ui is initialized here
        initLayoutReferences();
        LngLat myMarker = new LngLat(51.6662873, 32.6458363);
        markerPoss = new ArrayList<LngLat>();
        addMarker(myMarker);
        addMarker(new LngLat(51.6662879, 32.6428369));
        addMarker(new LngLat(51.6635821, 32.6458399));
        initLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPermissionGranted()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        lastLOc = null;
    }

    // We use findViewByID for every element in our layout file here
    private void initViews() {
        map = findViewById(R.id.map);
    }

    // Initializing map
    private void initMap() {
        // Creating a VectorElementLayer(called markerLayer) to add all markers to it and adding it to map's layers
        markerLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(markerLayer);
        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY, String.valueOf(getCacheDir()), 10));
        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.6662873, 32.6458363), 0);
        map.setZoom(14, 0);
    }

    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d("asmr", "initLocation: ");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                userLocation = locationResult.getLastLocation();
                Toast.makeText(MainActivity.this, userLocation.toString(), Toast.LENGTH_SHORT).show();
                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                onLocationChange();
            }
        };

        mRequestingLocationUpdates = false;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

    }


    // Initializing layout references (views, map and map events)
    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();
        markerLayer.setVectorElementEventListener(new VectorElementEventListener() {
            @Override
            public boolean onVectorElementClicked(ElementClickData clickInfo) {
                Handler mhandler = new Handler(Looper.getMainLooper());
                mhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "شما نقطه را لمس کردید.", Toast.LENGTH_SHORT).show();
                    }
                });
                return super.onVectorElementClicked(clickInfo);
            }
        });
        map.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapClicked(ClickData mapClickInfo) {
                if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_SINGLE) {
                    // by calling getClickPos(), we can get position of clicking (or tapping)
                    LngLat clickedLocation = mapClickInfo.getClickPos();
                    // addMarker adds a marker (pretty self explanatory :D) to the clicked location
                    addMarker(clickedLocation);
                }
            }
        });

    }

    // This method gets a LngLat as input and adds a marker on that position
    private void addMarker(LngLat loc) {
        AnimationStyleBuilder animStBl = new AnimationStyleBuilder();
        animStBl.setFadeAnimationType(AnimationType.ANIMATION_TYPE_SMOOTHSTEP);
        animStBl.setSizeAnimationType(AnimationType.ANIMATION_TYPE_SPRING);
        animStBl.setPhaseInDuration(0.2f);
        animStBl.setPhaseOutDuration(0.2f);
        AnimationStyle animSt = animStBl.buildStyle();

        // Creating marker style. We should use an object of type MarkerStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type MarkerStyle
        MarkerStyleCreator markStCr = new MarkerStyleCreator();
        markStCr.setSize(30f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker)));
        // AnimationStyle object - that was created before - is used here
        markStCr.setAnimationStyle(animSt);
        MarkerStyle markSt = markStCr.buildStyle();

        // Creating marker
        Marker marker = new Marker(loc, markSt);

        // Adding marker to markerLayer, or showing marker on map!
        markerLayer.add(marker);
        LngLat asd = loc;
        Log.d("tag", "addMarker: " + loc.toString());
        markerPoss.add(loc);

    }

    public void focusOnUserLocation(View view) {
        if (userLocation != null) {
            map.setFocalPointPosition(new LngLat(userLocation.getLongitude(), userLocation.getLatitude()), 0.25f);
            map.setZoom(15, 0.25f);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
                break;
            }
        }
    }

    /**
     * check device is upper marshmallow version then request permission
     */
    @SuppressLint("MissingPermission")
    private void checkLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        onLocationChange();
    }

    public void stopLocationUpdates() {
        // Removing location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void startReceivingLocationUpdates() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        checkLocationPermission();
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 200) {
            if (resultCode == Activity.RESULT_OK) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        }
    }

    private void onLocationChange() {
        Log.d("asmr", "onLocationChange: ");
        if (userLocation != null) {
            Log.d("asmr", "ininLocationChange: ");
            addUserMarker(new LngLat(userLocation.getLongitude(), userLocation.getLatitude()));
        }
    }

    private void addUserMarker(LngLat loc) {
        // Creating marker style. We should use an object of type MarkerStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type MarkerStyle
        Log.d("asmr", "addUserMarker: in add user marker");
        MarkerStyleCreator markStCr = new MarkerStyleCreator();
        markStCr.setSize(18f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cur_loc)));
        MarkerStyle markSt = markStCr.buildStyle();

        // Creating user marker
        Marker marker = new Marker(loc, markSt);
        // Clearing userMarkerLayer
        if (lastLOc != null) {
            Log.d("asmr", "addUserMarker: befor del");
            boolean b = markerLayer.remove(lastLOc);
        }
        lastLOc = marker;
        markerLayer.add(marker);
        Log.d("asmr", "addUserMarker: add done");

    }

    private boolean isPermissionGranted() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }


}
