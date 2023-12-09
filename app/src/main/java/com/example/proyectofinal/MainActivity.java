package com.example.proyectofinal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri photoUri;

    private RecyclerView recyclerView;
    private List<String> imageUrls;
    private MyRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    // Lógica para eliminar elementos seleccionados
                    return true;
                // Añadir más casos según sea necesario
            }
            return false;
        });

        // Botón para tomar la foto
        FloatingActionButton takePhotoButton = findViewById(R.id.fab_add_photo);
        takePhotoButton.setOnClickListener(view -> dispatchTakePictureIntent());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        imageUrls = new ArrayList<>();
        adapter = new MyRecyclerViewAdapter(this, imageUrls);
        recyclerView.setAdapter(adapter);

        loadImagesFromFirebase();
        adapter.setOnItemLongClickListener(new MyRecyclerViewAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClicked(int position) {
                boolean newState = !adapter.isItemSelected(position);
                adapter.setItemSelected(position, newState);
                adapter.notifyItemChanged(position);

                if (adapter.anyItemSelected()) {
                    toolbar.setVisibility(View.VISIBLE);
                } else {
                    toolbar.setVisibility(View.GONE);
                }
            }
        });



    }

    private void loadImagesFromFirebase() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("images");
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                imageUrls.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String imageUrl = snapshot.getValue(String.class);
                    imageUrls.add(imageUrl);
                    Log.d("FirebaseData", "Image URL: " + imageUrl); // Añade un mensaje de log
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("FirebaseData", "Error: " + databaseError.getMessage()); // Log para errores
            }
        });
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Convertir bitmap a Uri (necesario para Firebase)
            photoUri = getImageUri(getApplicationContext(), imageBitmap);
            uploadImageToFirebase();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase() {
        if (photoUri != null) {
            StorageReference fileRef = storageReference.child("imagenes/" + UUID.randomUUID().toString());
            fileRef.putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            // Guarda la URL de la imagen en Firebase Realtime Database
                            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("images");
                            String imageId = databaseRef.push().getKey();
                            databaseRef.child(imageId).setValue(imageUrl);

                            Toast.makeText(MainActivity.this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                    });
        }
    }

}