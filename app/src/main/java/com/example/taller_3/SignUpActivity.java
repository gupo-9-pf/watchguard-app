package com.example.taller_3;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.taller_3.model.User;
import com.example.taller_3.util.Image;
import com.example.taller_3.util.Permissions;
import com.example.taller_3.util.Validations;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileNotFoundException;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignUpActivity extends AppCompatActivity {
    public static final String LOCATION_PERMISSION_NAME = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final int LOCATION_PERMISSION_ID = 1;
    public static final String CAMERA_PERMISSION_NAME = Manifest.permission.CAMERA;
    public static final int CAMERA_PERMISSION_ID = 2;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final String GALLERY_PERMISSION_NAME = Manifest.permission.READ_EXTERNAL_STORAGE;
    public static final int GALLERY_PERMISSION_ID = 3;

    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;
    TextInputEditText identification, name, lastName, email, password;
    FloatingActionButton camera, gallery;
    CircleImageView image;
    Uri profileUri = null;
    Button submit;
    Double latitude, longitude;

    ActivityResultLauncher<String> getGalleryContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            try {
                image.setImageBitmap(BitmapFactory.decodeStream(getContentResolver().openInputStream(result)));
                profileUri = Image.bitMapToUri(SignUpActivity.this, BitmapFactory.decodeStream(getContentResolver().openInputStream(result)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        auth = FirebaseAuth.getInstance();
        image = findViewById(R.id.imageProfile);
        identification = findViewById(R.id.editTextIdentification);
        name = findViewById(R.id.editTextName);
        lastName = findViewById(R.id.editTextLastName);
        email = findViewById(R.id.editTextEmail);
        password = findViewById(R.id.editTextPassword);
        camera = findViewById(R.id.floatingCamera);
        gallery = findViewById(R.id.floatingGallery);
        submit = findViewById(R.id.buttonSubmit);
        Permissions.requestPermission(SignUpActivity.this, LOCATION_PERMISSION_NAME, "", LOCATION_PERMISSION_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) afterCameraPermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            image.setImageBitmap((Bitmap) data.getExtras().get("data"));
            profileUri = Image.bitMapToUri(SignUpActivity.this, (Bitmap) data.getExtras().get("data"));
        }
    }

    public void onSubmitClicked(View view) {
        if (validateFields()) {
            getLocation();
            createFirebaseUser(email.getText().toString(), password.getText().toString());
        }
    }

    public void onCameraClicked(View view) {
        Permissions.requestPermission(SignUpActivity.this, CAMERA_PERMISSION_NAME, "", CAMERA_PERMISSION_ID);
        afterCameraPermission();
    }

    public void onGalleryClicked(View view) {
        Permissions.requestPermission(SignUpActivity.this, GALLERY_PERMISSION_NAME, "", GALLERY_PERMISSION_ID);
        if (Permissions.permissionGranted(SignUpActivity.this, GALLERY_PERMISSION_NAME))
            getGalleryContent.launch("image/*");
    }

    private boolean validateFields() {
        return Validations.validateEmptyField(identification) &&
                Validations.validateEmptyField(name) &&
                Validations.validateEmptyField(lastName) &&
                Validations.validateEmptyField(email) &&
                Validations.validateEmailFormat(email) &&
                Validations.validateEmptyField(password);
    }

    private void afterCameraPermission() {
        if (Permissions.permissionGranted(SignUpActivity.this, CAMERA_PERMISSION_NAME))
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE);
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(SignUpActivity.this, LOCATION_PERMISSION_NAME) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this,
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null && Permissions.permissionGranted(SignUpActivity.this, LOCATION_PERMISSION_NAME)) {
                                longitude = location.getLongitude();
                                latitude = location.getLatitude();
                            }
                        }
                    });
        }
    }

    private void createFirebaseUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    saveUser();
                    saveImage();
                }
            }
        });
    }

    private void saveUser() {
        User user = createObject();
        FirebaseDatabase.getInstance().getReference("users")
                .child(auth.getCurrentUser().getUid())
                .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                    startActivity(new Intent(SignUpActivity.this, HomeActivity.class));
                else
                    Toast.makeText(SignUpActivity.this, "Invalid registration", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImage() {
        StorageReference sRef = FirebaseStorage.getInstance().getReference().child("users/" + auth.getCurrentUser().getUid());
        sRef.putFile(profileUri);
    }

    private User createObject() {
        return new User(
                identification.getText().toString(),
                name.getText().toString(),
                lastName.getText().toString(),
                email.getText().toString(),
                password.getText().toString(),
                latitude,
                longitude
        );
    }
}