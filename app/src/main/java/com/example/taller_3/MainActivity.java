package com.example.taller_3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    TextInputEditText email, password;
    Button submit;
    TextView signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        email = findViewById(R.id.editTextEmail);
        password = findViewById(R.id.editTextPassword);
        submit = findViewById(R.id.buttonSubmit);
        signUp = findViewById(R.id.textSignUp);
        isUserSignedIn(auth.getCurrentUser());
    }

    public void onSignInClicked(View view) {
        if (validateIfFieldIsEmpty(email) && validateIfFieldIsEmpty(password))
            firebaseAuthentication(email.getText().toString(), password.getText().toString());
    }

    public void onSignUpClicked(View view) {
        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
    }

    private boolean validateIfFieldIsEmpty(TextInputEditText input) {
        boolean flag = true;
        String value = input.getText().toString();
        if (value.isEmpty()) {
            input.setError("Please write something");
            input.requestFocus();
            flag = false;
        }
        return flag;
    }

    private void firebaseAuthentication(String email, String password) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) updateUi(auth.getCurrentUser());
                else
                    Toast.makeText(MainActivity.this, "Authentication failure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void isUserSignedIn(FirebaseUser user) {
        if (user != null)
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
    }

    private void updateUi(FirebaseUser user) {
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            ref.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DataSnapshot> task) {
                    if (task.isSuccessful())
                        if(task.getResult().exists())
                            startActivity(new Intent(MainActivity.this, HomeActivity.class));
                }
            });
        } else {
            email.setText("");
            password.setText("");
        }
    }
}