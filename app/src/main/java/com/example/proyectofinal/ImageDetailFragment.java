package com.example.proyectofinal;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ImageDetailFragment extends Fragment {

    private String imagePath;
    private String imageName;

    public ImageDetailFragment(String imagePath, String imageName) {
        this.imagePath = imagePath;
        this.imageName = imageName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_detail, container, false);

        // Configura la vista aquí (p.ej., establecer el nombre de la imagen y el listener del botón)

        return view;
    }

    // Método para eliminar la imagen
    private void deleteImage() {
        // Lógica para eliminar la imagen
    }
}
