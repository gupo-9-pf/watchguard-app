package com.example.taller_3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.example.taller_3.adapter.AvailableUserAdapter;
import com.example.taller_3.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AvailableUsersActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseDatabase db;
    ArrayList<User> availableUsers;
    ListView availableUsersList;
    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_users);
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        addToAvailable();
        availableUsersList = (ListView) findViewById(R.id.listAvailableUsers);
    }

    private void addToAvailable() {
        DatabaseReference ref = db.getReference("availableUsers");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableUsers = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren())
                    if (!data.getKey().equals(auth.getUid())) {
                        getProfilePic(data.getKey());
                        User user = data.getValue(User.class);
                        user.setProfileImage(photoUri);
                        availableUsers.add(user);
                    }
                availableUsersList.setAdapter(new AvailableUserAdapter(AvailableUsersActivity.this, R.layout.item_available_user, availableUsers));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void getProfilePic(String id) {
        final Uri[] uriReturn = new Uri[1];
        StorageReference sRef = FirebaseStorage.getInstance().getReference("/users");
        try {
            File localFile = File.createTempFile("images", "jpg");
            StorageReference imageRef = sRef.child(id);
            imageRef.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            photoUri = Uri.fromFile(localFile);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    photoUri = null;
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}