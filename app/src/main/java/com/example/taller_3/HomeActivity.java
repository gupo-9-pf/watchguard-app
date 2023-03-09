package com.example.taller_3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.taller_3.model.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Scanner;

public class HomeActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    FirebaseAuth auth;
    FirebaseDatabase db;
    MaterialToolbar menuToolBar;
    Double currentUserLatitude = 0d, currentUserLongitude = 0d;
    Marker currentUserMarker;
    MapView map;
    User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = notifyCallbackChanges();
        locationRequest = createLocationRequest();
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        menuToolBar = findViewById(R.id.appBarMenu);
        setSupportActionBar(menuToolBar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getUserLocationFromDb();
        readJson();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        stopLocationUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_bar_tools, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Switch mSwitch = menu.findItem(R.id.appBarSwitch).getActionView().findViewById(R.id.switchOptions);
        checkIfUserAlreadyAvailable(mSwitch);
        changeSwitchState(mSwitch);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.appBarLogOut:
                auth.signOut();
                startActivity(new Intent(HomeActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            case R.id.appBarUsers:
                startActivity(new Intent(HomeActivity.this, AvailableUsersActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkIfUserAlreadyAvailable(Switch mSwitch) {
        FirebaseDatabase.getInstance().getReference("availableUsers").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                for (DataSnapshot snapshot : task.getResult().getChildren())
                    if (snapshot.getKey().equals(auth.getUid()))
                        mSwitch.setChecked(true);
            }
        });
    }

    private void changeSwitchState(Switch mSwitch) {
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    addToAvailable();
                    Toast.makeText(HomeActivity.this, "Available", Toast.LENGTH_SHORT).show();
                } else {
                    db.getReference("availableUsers").child(auth.getUid()).removeValue();
                    Toast.makeText(HomeActivity.this, "Not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private LocationCallback notifyCallbackChanges() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentUserLatitude = location.getLatitude();
                    currentUserLongitude = location.getLongitude();
                    if (currentUserMarker == null) {
                        currentUserMarker = createMarker(new GeoPoint(currentUserLatitude, currentUserLongitude),
                                "",
                                "",
                                R.drawable.ic_user_marker_blue);
                        placeMarker(currentUserMarker);
                    } else {
                        currentUserMarker.setPosition(new GeoPoint(currentUserLatitude, currentUserLongitude));
                        setUserCurrentLocationOnDb();
                    }
                }
            }
        };
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private Marker createMarker(GeoPoint p, String title, String desc, int iconID) {
        Marker marker = null;
        if (map != null) {
            marker = new Marker(map);
            if (title != null) marker.setTitle(title);
            if (desc != null) marker.setSubDescription(desc);
            if (iconID != 0) {
                Drawable myIcon = getResources().getDrawable(iconID, this.getTheme());
                marker.setIcon(myIcon);
            }
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        return marker;
    }

    private void placeMarker(Marker marker) {
        map.getOverlays().add(marker);
    }


    private void readJson() {
        Scanner sc = new Scanner(getResources().openRawResource(R.raw.locations));
        StringBuilder builder = new StringBuilder();
        while (sc.hasNextLine())
            builder.append(sc.nextLine());
        parseJson(builder.toString());
    }

    private void parseJson(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray locations = root.getJSONArray("locationsArray");
            for (int i = 0; i < locations.length(); i++) {
                Marker marker = createMarker(new GeoPoint(Double.parseDouble(locations.getJSONObject(i).getString("latitude")),
                                Double.parseDouble(locations.getJSONObject(i).getString("longitude"))),
                        locations.getJSONObject(i).getString("name"),
                        "",
                        R.drawable.ic_marker_red);
                placeMarker(marker);
            }
        } catch (Exception e) {
            Log.e("ERROR", "There was an error");
        }
    }

    private void addToAvailable() {
        DatabaseReference ref = db.getReference("users").child(auth.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getKey().equals(auth.getUid()))
                    db.getReference("availableUsers").child(auth.getUid()).setValue(snapshot.getValue(User.class));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void getUserLocationFromDb() {
        DatabaseReference ref = db.getReference("users").child(auth.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getKey().equals(auth.getUid())) {
                    currentUser = snapshot.getValue(User.class);
                    currentUserLatitude = currentUser.getLatitude();
                    currentUserLongitude = currentUser.getLongitude();
                    IMapController mapController = map.getController();
                    mapController.setZoom(13.0);
                    mapController.setCenter(new GeoPoint(currentUserLatitude, currentUserLongitude));
                    menuToolBar.setTitle("Current user: " + currentUser.getName() + " " + currentUser.getLastName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setUserCurrentLocationOnDb() {
        DatabaseReference ref = db.getReference("users").child(auth.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getKey().equals(auth.getUid())) {
                    User user = snapshot.getValue(User.class);
                    user.setLatitude(currentUserLatitude);
                    user.setLongitude(currentUserLongitude);
                    db.getReference("users").child(auth.getUid()).setValue(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}