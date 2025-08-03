package com.kopecode.palmastour.ui.gallery;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kopecode.palmastour.R;
import com.kopecode.palmastour.data.SupabaseClient;
import com.kopecode.palmastour.model.Location;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locations;
    private final OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(Location location);
        void onDeleteLocationClick(Location location, int position);
    }

    public LocationAdapter(List<Location> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    public void updateLocations(List<Location> newLocations) {
        this.locations = newLocations;
        notifyDataSetChanged();
    }
    
    public void removeLocation(int position) {
        if (position >= 0 && position < locations.size()) {
            locations.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, locations.size());
        }
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locations.get(position);
        holder.bind(location, listener, position);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textDate;
        private final TextView textCoordinates;
        private final TextView textPhotoCount;
        private final Button buttonDelete;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_location_name);
            textDate = itemView.findViewById(R.id.text_location_date);
            textCoordinates = itemView.findViewById(R.id.text_location_coordinates);
            textPhotoCount = itemView.findViewById(R.id.text_photos_count);
            buttonDelete = itemView.findViewById(R.id.button_delete_location);
        }

        public void bind(final Location location, final OnLocationClickListener listener, final int position) {
            textName.setText(location.getName());
            
            // Formatar a data
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = location.getCreatedAt();
            if (date != null) {
                textDate.setText(outputFormat.format(date));
            } else {
                textDate.setText("");
            }
            
            // Formatar as coordenadas
            String coordinates = String.format(Locale.getDefault(), 
                    "%.6f, %.6f", location.getLatitude(), location.getLongitude());
            textCoordinates.setText(coordinates);
            
            // Contar fotos
            int photoCount = location.getPhotos() != null ? location.getPhotos().size() : 0;
            textPhotoCount.setText(String.format(Locale.getDefault(), 
                    "Fotos: %d", photoCount));
            
            // Configurar o clique no item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLocationClick(location);
                }
            });
            
            // Configurar o clique no botão de exclusão
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteLocationClick(location, position);
                }
            });
        }
    }
}