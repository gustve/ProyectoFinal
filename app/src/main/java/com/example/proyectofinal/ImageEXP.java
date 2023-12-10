package com.example.proyectofinal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ImageEXP extends AppCompatActivity {
    private String imageUrl, imageName;
     ImageView ivImagen;
     TextView tvImageName;
     Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_image_detail);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            imageUrl  = bundle.getString("imagePath");
            imageName = bundle.getString("imageName");

            ivImagen = findViewById(R.id.ivImagen);
            tvImageName = findViewById(R.id.imageName);
            btnDelete = findViewById(R.id.btnDelete);

            Glide.with(this).load(imageUrl).into(ivImagen);
            tvImageName.setText(imageName);
        }
        btnDelete.setOnClickListener(v -> deleteImage());
    }

    private void deleteImage() {
        StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        photoRef.delete().addOnSuccessListener(aVoid -> {
            // Image deleted successfully
            removeFromDatabase();
        }).addOnFailureListener(e -> {
            // Handle any errors here
            Toast.makeText(ImageEXP.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private void removeFromDatabase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("images");
        dbRef.orderByValue().equalTo(imageUrl).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue();
                }
                finish(); // Close this activity and return to the previous screen
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Error: " + databaseError.getMessage());
                // Optionally, you may want to stay on this screen if the database update fails
            }
        });
    }
}
