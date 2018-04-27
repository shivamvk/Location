package com.example.shivamvk.location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 123;
    TextView tvLocation;

    FusedLocationProviderClient fusedLocationProviderClient;
    Location mLastLocation;
    String mAddressOutput;
    private static final int ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;

    private static String REQUEST_URL =
            "https://maps.googleapis.com/maps/api/geocode/json?key=AIzaSyA8awdav8Of2pNy-wFo98yhU33Sn48WxVk&latlng=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tv_location);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        findViewById(R.id.bt_search_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .build(MainActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.i("createError", e.getMessage());
                }
            }
        });

        findViewById(R.id.bt_locate_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            ACCESS_FINE_LOCATION);
                } else {
                    if (getLocationMode(MainActivity.this) != 0) {
                        getLocation();
                    } else {
                        displayLocationSettingsRequest(MainActivity.this);
                    }
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case ACCESS_FINE_LOCATION:
                if (getLocationMode(MainActivity.this) != 0) {
                    getLocation();
                } else {
                    displayLocationSettingsRequest(MainActivity.this);
                }
        }
    }
    /**
     * Method is used to check which locatrion mode is selected,
     * @return If return 0 = LOCATION_MODE_OFF, 1 =  LOCATION_MODE_SENSORS_ONLY & DEVICE_ONLY, 2 = LOCATION_MODE_BATTERY_SAVING , 3 = LOCATION_MODE_HIGH_ACCURACY
     */

    public static int getLocationMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        } else {
            try {
                String locationProviders = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                );
                if (!TextUtils.isEmpty(locationProviders)) {
                    return 2;
                } else {
                    return 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    private void getLocation(){
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        mLastLocation = location;
                        Toast.makeText(MainActivity.this, "finding you...", Toast.LENGTH_SHORT).show();
                        makeAddress();
                    }
                });
    }

    private void makeAddress() {
        if (mLastLocation == null){
            Toast.makeText(this, "Error! Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        REQUEST_URL = REQUEST_URL + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude();
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                REQUEST_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray results = jsonObject.getJSONArray("results");
                            JSONObject addressObject = results.getJSONObject(0);
                            mAddressOutput = addressObject.getString("formatted_address");
                            tvLocation.setText(mAddressOutput);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i("error", "Place: " + place.getName());
                tvLocation.setText(place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("error", status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK){
                getLocation();
            } else {
                displayLocationSettingsRequest(MainActivity.this);
            }
        }
    }
}
