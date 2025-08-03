package com.kopecode.palmastour.ui.slideshow;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kopecode.palmastour.R;
import com.kopecode.palmastour.data.SupabaseClient;
import com.kopecode.palmastour.model.Photo;
import com.kopecode.palmastour.ui.slideshow.SlideshowViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Photo> photos;
    private SlideshowViewModel viewModel;

    public PhotoAdapter(List<Photo> photos) {
        this.photos = photos;
        this.viewModel = null;
    }
    
    public PhotoAdapter(List<Photo> photos, SlideshowViewModel viewModel) {
        this.photos = photos;
        this.viewModel = viewModel;
    }

    public void updatePhotos(List<Photo> newPhotos) {
        this.photos = newPhotos;
        notifyDataSetChanged();
    }
    
    public List<Photo> getPhotos() {
        return photos;
    }
    
    public void removePhoto(int position) {
        if (position >= 0 && position < photos.size()) {
            photos.remove(position);
            notifyItemRemoved(position);
            
            // Verificar se a lista está vazia e atualizar o ViewModel se disponível
            if (photos.isEmpty() && viewModel != null) {
                viewModel.setPhotos(new ArrayList<>());
            }
        }
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);
        holder.bind(photo);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imagePhoto;
        private final TextView textDate;
        private final TextView textDescription;
        private final Button btnEditPhoto;
        private final Button btnDeletePhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.image_photo);
            textDate = itemView.findViewById(R.id.text_photo_date);
            textDescription = itemView.findViewById(R.id.text_photo_description);
            btnEditPhoto = itemView.findViewById(R.id.btn_edit_photo);
            btnDeletePhoto = itemView.findViewById(R.id.btn_delete_photo);
        }

        public void bind(final Photo photo) {
            // Carregar a imagem usando Glide
            if (photo.getStorageUrl() != null && !photo.getStorageUrl().isEmpty()) {
                // Se tiver URL de armazenamento, carrega da URL
                Glide.with(itemView.getContext())
                        .load(photo.getStorageUrl())
                        .placeholder(R.drawable.ic_menu_gallery)
                        .error(R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(imagePhoto);
            } else if (photo.getFilePath() != null && !photo.getFilePath().isEmpty()) {
                // Se tiver caminho de arquivo local, carrega do arquivo
                Glide.with(itemView.getContext())
                        .load(photo.getFilePath())
                        .placeholder(R.drawable.ic_menu_gallery)
                        .error(R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(imagePhoto);
            } else {
                // Fallback para placeholder
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(imagePhoto);
            }
            
            // Formatar a data
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = photo.getCreatedAt();
            if (date != null) {
                textDate.setText(outputFormat.format(date));
            } else {
                textDate.setText("");
            }
            
            // Definir a descrição
            textDescription.setText(photo.getDescription());
            
            // Configurar o botão de edição
            btnEditPhoto.setOnClickListener(v -> showEditDialog(photo));
            
            // Configurar o botão de exclusão
            btnDeletePhoto.setOnClickListener(v -> showDeleteConfirmationDialog(photo));
        }
        
        private void showDeleteConfirmationDialog(final Photo photo) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle(R.string.confirm_delete_title);
            builder.setMessage(R.string.confirm_delete_message);
            
            // Configurar os botões
            builder.setPositiveButton(R.string.delete_photo, (dialog, which) -> {
                deletePhoto(photo);
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
            
            builder.show();
        }
        
        private void deletePhoto(final Photo photo) {
            // Iniciar uma thread para excluir a foto no Supabase
            new Thread(() -> {
                try {
                    SupabaseClient client = SupabaseClient.getInstance(itemView.getContext());
                    boolean success = client.deletePhoto(photo.getId());
                    
                    // Atualizar a UI na thread principal
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        if (success) {
                            // Remover a foto da lista e notificar o adaptador
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION) {
                                // Remover a foto da lista
                                RecyclerView recyclerView = (RecyclerView) itemView.getParent();
                                if (recyclerView != null) {
                                    RecyclerView.Adapter adapter = recyclerView.getAdapter();
                                    if (adapter instanceof PhotoAdapter) {
                                        PhotoAdapter photoAdapter = (PhotoAdapter) adapter;
                                        photoAdapter.removePhoto(position);
                                    }
                                }
                                
                                Toast.makeText(itemView.getContext(), R.string.photo_deleted, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(itemView.getContext(), R.string.error_deleting_photo, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        Toast.makeText(itemView.getContext(), R.string.error_deleting_photo, Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
        
        private void showEditDialog(final Photo photo) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            builder.setTitle(R.string.edit_photo_description_title);
            
            // Configurar o layout do diálogo
            final EditText input = new EditText(itemView.getContext());
            input.setText(photo.getDescription());
            builder.setView(input);
            
            // Configurar os botões
            builder.setPositiveButton(R.string.save, (dialog, which) -> {
                String newDescription = input.getText().toString().trim();
                updatePhotoDescription(photo, newDescription);
            });
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
            
            builder.show();
        }
        
        private void updatePhotoDescription(final Photo photo, final String newDescription) {
            // Atualizar a descrição localmente
            photo.setDescription(newDescription);
            textDescription.setText(newDescription);
            
            // Atualizar no Supabase
            new Thread(() -> {
                try {
                    SupabaseClient client = SupabaseClient.getInstance(itemView.getContext());
                    boolean success = client.updatePhoto(photo);
                    
                    // Atualizar a UI na thread principal
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(itemView.getContext(), R.string.photo_description_updated, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(itemView.getContext(), R.string.error_updating_photo_description, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((android.app.Activity) itemView.getContext()).runOnUiThread(() -> {
                        Toast.makeText(itemView.getContext(), itemView.getContext().getString(R.string.error_updating_photo_description) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }
}