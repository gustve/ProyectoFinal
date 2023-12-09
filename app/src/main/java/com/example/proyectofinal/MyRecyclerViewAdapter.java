package com.example.proyectofinal;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;


import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private List<String> mData;
    private LayoutInflater mInflater;
    private Context context;
    private OnItemLongClickListener longClickListener;

    MyRecyclerViewAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.mData = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.imageholder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String imageUrl = mData.get(position);
        Glide.with(context)
                .load(imageUrl)
                .into(holder.imageView);

        holder.checkBox.setChecked(isItemSelected(position));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setItemSelected(position, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public interface OnItemLongClickListener {
        void onItemLongClicked(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    private SparseBooleanArray selectedItems = new SparseBooleanArray();

    public boolean isItemSelected(int position) {
        return selectedItems.get(position, false);
    }

    public void setItemSelected(int position, boolean isSelected) {
        if (isSelected) {
            selectedItems.put(position, true);
        } else {
            selectedItems.delete(position);
        }
    }

    public boolean anyItemSelected() {
        return selectedItems.size() > 0;
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            checkBox = itemView.findViewById(R.id.checkBox);

            // Establece el OnLongClickListener aquí
            itemView.setOnLongClickListener(view -> {
                checkBox.setVisibility(View.VISIBLE);
                if (longClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        longClickListener.onItemLongClicked(position);
                    }
                }
                return true;
            });
        }
    }
}
