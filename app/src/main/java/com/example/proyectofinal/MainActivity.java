package com.example.proyectofinal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

    private boolean isSelectionMode = false;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private StorageReference storageReference;
    private Uri photoUri;

    private List<String> imageUrls;
    private MyRecyclerViewAdapter adapter;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Botón para tomar la foto
        FloatingActionButton takePhotoButton = findViewById(R.id.fab_add_photo);
        takePhotoButton.setOnClickListener(view -> dispatchTakePictureIntent());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        imageUrls = new ArrayList<>();
        adapter = new MyRecyclerViewAdapter(this, imageUrls);
        recyclerView.setAdapter(adapter);

        loadImagesFromFirebase();
        adapter.setOnItemLongClickListener(position -> {
            isSelectionMode = true;
            adapter.setSelectionMode(true);
            adapter.notifyDataSetChanged(); // Notifica al adaptador para actualizar la vista
            toolbar.setVisibility(View.VISIBLE);
        });
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (isSelectionMode) {
                    isSelectionMode = false;
                    adapter.clearSelections();
                    adapter.setSelectionMode(false);
                    adapter.notifyDataSetChanged();
                    toolbar.setVisibility(View.GONE);
                } else {
                    setEnabled(false); // Desactiva este callback
                    getOnBackPressedDispatcher().onBackPressed(); // Llama al comportamiento por defecto
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            eliminarFotosSeleccionadas();
            isSelectionMode = false;
            adapter.clearSelections();
            adapter.setSelectionMode(false);
            adapter.notifyDataSetChanged();
            toolbar.setVisibility(View.GONE);
            return true;
        } else {
            return false;
        }
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
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            someActivityResultLauncher.launch(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            assert extras != null;
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Convertir bitmap a Uri (necesario para Firebase)
            assert imageBitmap != null;
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
                            assert imageId != null;
                            databaseRef.child(imageId).setValue(imageUrl);

                            Toast.makeText(MainActivity.this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error al subir la imagen", Toast.LENGTH_SHORT).show());
        }
    }

    private void eliminarFotosSeleccionadas() {
        List<String> fotosSeleccionadas = adapter.getFotosSeleccionadas();

        for (String fotoUrl : fotosSeleccionadas) {
            // Eliminar la imagen de Firebase Storage
            StorageReference fotoRef = FirebaseStorage.getInstance().getReferenceFromUrl(fotoUrl);
            fotoRef.delete().addOnSuccessListener(aVoid -> {
                // Imagen eliminada de Firebase Storage
                // Ahora elimina la referencia de la imagen de Firebase Realtime Database
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("images");
                dbRef.orderByValue().equalTo(fotoUrl).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("Firebase", "Error al eliminar la imagen", databaseError.toException());
                    }
                });
            }).addOnFailureListener(e -> {
                // Manejar el error
                Log.e("Firebase", "Error al eliminar la imagen", e);
            });
        }

        // Recargar y actualizar el RecyclerView
        loadImagesFromFirebase();
    }

    private ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Bundle extras = data.getExtras();
                    assert extras != null;
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    // Convertir bitmap a Uri (necesario para Firebase)
                    assert imageBitmap != null;
                    photoUri = getImageUri(getApplicationContext(), imageBitmap);
                    uploadImageToFirebase();

                }
            });
    public void openImageDetailFragment(String imagePath, String imageName) {
        ImageDetailFragment fragment = new ImageDetailFragment(imagePath, imageName);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment) // Asegúrate de tener un contenedor de fragmentos en tu layout
                .addToBackStack(null)
                .commit();
    }
}