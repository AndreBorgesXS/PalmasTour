package com.kopecode.palmastour.ui.gallery;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kopecode.palmastour.R;
import com.kopecode.palmastour.data.SupabaseClient;
import com.kopecode.palmastour.databinding.FragmentGalleryBinding;
import com.kopecode.palmastour.model.Location;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment implements LocationAdapter.OnLocationClickListener {

    private FragmentGalleryBinding binding;
    private GalleryViewModel galleryViewModel;
    private LocationAdapter locationAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        galleryViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Configurar o RecyclerView
        binding.recyclerLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        locationAdapter = new LocationAdapter(new ArrayList<>(), this);
        binding.recyclerLocations.setAdapter(locationAdapter);

        // Observar mudanças na lista de localizações
        galleryViewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            locationAdapter.updateLocations(locations);
            updateEmptyView(locations);
        });

        // Observar estado de carregamento
        galleryViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Carregar localizações
        loadLocations();

        return root;
    }

    private void loadLocations() {
        galleryViewModel.setIsLoading(true);
        
        new Thread(() -> {
            try {
                SupabaseClient client = SupabaseClient.getInstance(requireContext());
                List<Location> locations = client.getLocations();
                
                requireActivity().runOnUiThread(() -> {
                    galleryViewModel.setLocations(locations);
                    galleryViewModel.setIsLoading(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), R.string.error_loading_locations, Toast.LENGTH_SHORT).show();
                    galleryViewModel.setIsLoading(false);
                });
            }
        }).start();
    }

    private void updateEmptyView(List<Location> locations) {
        if (locations == null || locations.isEmpty()) {
            binding.textNoLocations.setVisibility(View.VISIBLE);
            binding.recyclerLocations.setVisibility(View.GONE);
        } else {
            binding.textNoLocations.setVisibility(View.GONE);
            binding.recyclerLocations.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLocationClick(Location location) {
        // Navegar para o fragmento de fotos com o ID da localização
        Bundle args = new Bundle();
        args.putString("locationId", location.getId());
        args.putString("locationName", location.getName());
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_gallery_to_nav_slideshow, args);
    }
    
    @Override
    public void onDeleteLocationClick(Location location, int position) {
        // Mostrar diálogo de confirmação
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_location_title)
                .setMessage(R.string.confirm_delete_location_message)
                .setPositiveButton(R.string.delete_location, (dialog, which) -> {
                    deleteLocation(location, position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void deleteLocation(Location location, int position) {
        galleryViewModel.setIsLoading(true);
        
        new Thread(() -> {
            try {
                SupabaseClient client = SupabaseClient.getInstance(requireContext());
                boolean success = client.deleteLocation(location.getId());
                
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        // Obter a lista atual de localizações
                        List<Location> locations = galleryViewModel.getLocations().getValue();
                        if (locations != null && position >= 0 && position < locations.size()) {
                            // Criar uma nova lista sem a localização removida
                            List<Location> updatedLocations = new ArrayList<>(locations);
                            updatedLocations.remove(position);
                            
                            // Atualizar o ViewModel com a nova lista
                            galleryViewModel.setLocations(updatedLocations);
                            
                            // Atualizar o adaptador
                            locationAdapter.updateLocations(updatedLocations);
                            
                            Toast.makeText(getContext(), R.string.location_deleted, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.error_deleting_location, Toast.LENGTH_SHORT).show();
                    }
                    galleryViewModel.setIsLoading(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), R.string.error_deleting_location, Toast.LENGTH_SHORT).show();
                    galleryViewModel.setIsLoading(false);
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