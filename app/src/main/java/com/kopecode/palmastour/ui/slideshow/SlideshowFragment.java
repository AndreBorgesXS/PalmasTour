package com.kopecode.palmastour.ui.slideshow;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.kopecode.palmastour.R;
import com.kopecode.palmastour.data.SupabaseClient;
import com.kopecode.palmastour.databinding.FragmentSlideshowBinding;
import com.kopecode.palmastour.model.Location;
import com.kopecode.palmastour.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private SlideshowViewModel slideshowViewModel;
    private PhotoAdapter photoAdapter;
    private String locationId;
    private String locationName;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        slideshowViewModel = new ViewModelProvider(this).get(SlideshowViewModel.class);

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Obter o ID da localização dos argumentos
        Bundle args = getArguments();
        if (args != null) {
            locationId = args.getString("locationId");
            locationName = args.getString("locationName");
            slideshowViewModel.setLocationId(locationId);
            slideshowViewModel.setLocationName(locationName);
        }

        // Configurar o título
        slideshowViewModel.getLocationName().observe(getViewLifecycleOwner(), name -> {
            binding.textLocationName.setText(name);
        });
        
        // Configurar o botão de edição
        binding.btnEditLocation.setOnClickListener(v -> {
            showEditLocationDialog();
        });

        // Configurar o RecyclerView
        binding.recyclerPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        photoAdapter = new PhotoAdapter(new ArrayList<>(), slideshowViewModel);
        binding.recyclerPhotos.setAdapter(photoAdapter);

        // Observar mudanças na lista de fotos
        slideshowViewModel.getPhotos().observe(getViewLifecycleOwner(), photos -> {
            photoAdapter.updatePhotos(photos);
            updateEmptyView(photos);
        });

        // Observar estado de carregamento
        slideshowViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Carregar fotos
        if (locationId != null) {
            loadPhotos(locationId);
        }

        return root;
    }

    private void loadPhotos(String locationId) {
        slideshowViewModel.setIsLoading(true);
        
        new Thread(() -> {
            try {
                SupabaseClient client = SupabaseClient.getInstance(requireContext());
                List<Photo> photos = client.getPhotosByLocation(locationId);
                
                requireActivity().runOnUiThread(() -> {
                    slideshowViewModel.setPhotos(photos);
                    slideshowViewModel.setIsLoading(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), R.string.error_loading_photos, Toast.LENGTH_SHORT).show();
                    slideshowViewModel.setIsLoading(false);
                });
            }
        }).start();
    }

    private void updateEmptyView(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            binding.textNoPhotos.setVisibility(View.VISIBLE);
            binding.recyclerPhotos.setVisibility(View.GONE);
        } else {
            binding.textNoPhotos.setVisibility(View.GONE);
            binding.recyclerPhotos.setVisibility(View.VISIBLE);
        }
    }

    private void showEditLocationDialog() {
        if (locationId == null || locationId.isEmpty()) {
            Toast.makeText(getContext(), R.string.location_id_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Criar um AlertDialog com um EditText para editar o nome da localização
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.edit_location_title);
        
        // Configurar o layout do diálogo
        final EditText input = new EditText(getContext());
        input.setText(locationName);
        input.setSelectAllOnFocus(true);
        builder.setView(input);
        
        // Configurar os botões
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateLocationName(newName);
            } else {
                Toast.makeText(getContext(), R.string.empty_name_error, Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        
        // Mostrar o diálogo
        builder.show();
    }
    
    private void updateLocationName(String newName) {
        if (locationId == null || locationId.isEmpty()) {
            Toast.makeText(getContext(), R.string.location_id_not_available, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Mostrar um indicador de progresso
        slideshowViewModel.setIsLoading(true);
        
        new Thread(() -> {
            try {
                SupabaseClient client = SupabaseClient.getInstance(requireContext());
                
                // Obter a localização atual
                Location location = client.getLocation(locationId);
                if (location == null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), R.string.location_not_found, Toast.LENGTH_SHORT).show();
                        slideshowViewModel.setIsLoading(false);
                    });
                    return;
                }
                
                // Atualizar o nome da localização
                location.setName(newName);
                
                // Salvar a localização atualizada
                boolean success = client.updateLocation(location);
                
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        // Atualizar o nome na UI
                        locationName = newName;
                        slideshowViewModel.setLocationName(newName);
                        Toast.makeText(getContext(), R.string.location_updated, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), R.string.error_updating_location, Toast.LENGTH_SHORT).show();
                    }
                    slideshowViewModel.setIsLoading(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), getString(R.string.error_updating_location) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    slideshowViewModel.setIsLoading(false);
                });
            }
        }).start();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}