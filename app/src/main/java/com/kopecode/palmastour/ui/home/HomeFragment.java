package com.kopecode.palmastour.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kopecode.palmastour.R;
import com.kopecode.palmastour.data.SupabaseClient;
import com.kopecode.palmastour.databinding.FragmentHomeBinding;
import com.kopecode.palmastour.model.Location;
import com.kopecode.palmastour.model.Photo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private String currentPhotoPath;
    private List<String> photosPaths = new ArrayList<>();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 3;

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Verificar se o arquivo da foto existe
                    File photoFile = new File(currentPhotoPath);
                    if (photoFile.exists() && photoFile.length() > 0) {
                        photosPaths.add(currentPhotoPath);
                        updatePhotosCount();
                        Toast.makeText(getContext(), "Foto salva em: " + currentPhotoPath, Toast.LENGTH_LONG).show();
                        
                        // Mostrar o botão SALVAR após tirar uma foto
                        binding.btnSaveWork.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getContext(), "Erro: Arquivo da foto não existe ou está vazio", Toast.LENGTH_LONG).show();
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(getContext(), "Captura de foto cancelada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Erro ao capturar foto: código " + result.getResultCode(), Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }
                
                // Verificar se as permissões solicitadas incluem a permissão da câmera
                if (allGranted) {
                    if (permissions.containsKey(Manifest.permission.CAMERA)) {
                        // Se a permissão da câmera foi concedida, verificar armazenamento
                        checkStoragePermission();
                    } else if (permissions.containsKey(Manifest.permission.READ_MEDIA_IMAGES) ||
                            permissions.containsKey(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        // Se as permissões de armazenamento foram concedidas, iniciar a câmera
                        dispatchTakePictureIntent();
                    } else {
                        // Outras permissões (como localização)
                        setupMap();
                    }
                } else {
                    Toast.makeText(getContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializa o cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Configura o mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configura os botões
        binding.btnCaptureLocation.setOnClickListener(v -> captureLocation());
        binding.btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        binding.btnSaveWork.setOnClickListener(v -> saveWork());

        // Inicialmente, ocultar os botões de tirar foto e salvar
        binding.btnTakePhoto.setVisibility(View.GONE);
        binding.btnSaveWork.setVisibility(View.GONE);

        // Verifica permissões
        checkLocationPermission();

        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupMap();
    }

    private void setupMap() {
        if (mMap == null) return;

        try {
            // Configurações básicas do mapa que não requerem permissão
            mMap.getUiSettings().setZoomControlsEnabled(true);
            
            // Verifica permissões de localização
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Habilita recursos de localização no mapa
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                
                // Obtém a localização atual com tratamento de erros
                try {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), (android.location.Location location) -> {
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            updateLocationInfo(currentLocation);
                        } else {
                            // Caso a localização seja nula, usa uma localização padrão (Palmas)
                            currentLocation = new LatLng(-10.2128, -48.3603); // Coordenadas de Palmas
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                            updateLocationInfo(currentLocation);
                            Toast.makeText(requireContext(), "Não foi possível obter sua localização atual", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {
                        // Em caso de falha, usa uma localização padrão
                        currentLocation = new LatLng(-10.2128, -48.3603); // Coordenadas de Palmas
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                        updateLocationInfo(currentLocation);
                        Toast.makeText(requireContext(), "Erro ao obter localização: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } catch (SecurityException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Erro ao acessar serviços de localização", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Se não tiver permissão, mostra uma localização padrão
                currentLocation = new LatLng(-10.2128, -48.3603); // Coordenadas de Palmas
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                Toast.makeText(requireContext(), "Permissão de localização não concedida", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Erro de segurança ao acessar o mapa", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Erro ao configurar o mapa", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Usar o launcher para solicitar permissão
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA
            });
            Toast.makeText(requireContext(), "É necessário permitir o acesso à câmera", Toast.LENGTH_LONG).show();
        } else {
            // Câmera permitida, verificar armazenamento
            checkStoragePermission();
        }
    }

    private void checkStoragePermission() {
        // Verificar a versão do Android para solicitar as permissões corretas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES
                });
                Toast.makeText(requireContext(), "É necessário permitir o acesso às imagens", Toast.LENGTH_LONG).show();
                return;
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // Android 12 e anteriores usam READ/WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                });
                Toast.makeText(requireContext(), "É necessário permitir o acesso ao armazenamento", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Se chegou aqui, todas as permissões foram concedidas
        dispatchTakePictureIntent();
    }

    // Não precisamos mais do onRequestPermissionsResult pois estamos usando o ActivityResultLauncher
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Delegamos para a implementação da superclasse
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void captureLocation() {
        if (mMap != null && currentLocation != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Localização atual"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            Toast.makeText(getContext(), R.string.location_saved, Toast.LENGTH_SHORT).show();
            
            // Mostrar o botão TIRAR FOTO após capturar a localização
            binding.btnTakePhoto.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getContext(), R.string.error_saving_location, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationInfo(LatLng location) {
        if (location != null) {
            String locationInfo = String.format(Locale.getDefault(),
                    "Localização atual: %.6f, %.6f", location.latitude, location.longitude);
            binding.textLocationInfo.setText(locationInfo);
        }
    }

    private void updatePhotosCount() {
        String photosCount = String.format(Locale.getDefault(),
                "Fotos: %d", photosPaths.size());
        binding.textPhotosCount.setText(photosCount);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Verificar se há um aplicativo de câmera disponível
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Toast.makeText(getContext(), "Arquivo criado: " + photoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(getContext(), "Erro ao criar arquivo: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                try {
                    Uri photoURI = FileProvider.getUriForFile(requireContext(),
                            "com.kopecode.palmastour.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureLauncher.launch(takePictureIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Erro ao configurar câmera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(getContext(), "Nenhum aplicativo de câmera encontrado", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        
        // Garantir que o diretório existe
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("Não foi possível acessar o diretório de armazenamento externo");
        }
        
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (!created) {
                throw new IOException("Não foi possível criar o diretório de armazenamento");
            }
        }
        
        // Verificar permissões de escrita
        if (!storageDir.canWrite()) {
            throw new IOException("Sem permissão de escrita no diretório de armazenamento");
        }
        
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void saveWork() {
        if (currentLocation == null) {
            Toast.makeText(getContext(), "Capture uma localização primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photosPaths.isEmpty()) {
            Toast.makeText(getContext(), "Tire pelo menos uma foto", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verificar conectividade de rede
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        
        if (!isConnected) {
            Toast.makeText(getContext(), "Sem conexão com a internet. Verifique sua conexão e tente novamente.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Indicar que o processo de salvamento começou
        homeViewModel.setIsSaving(true);
        Toast.makeText(getContext(), "Iniciando salvamento...", Toast.LENGTH_SHORT).show();

        // Preparar dados para salvar no Supabase
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());
        String locationName = "Localização " + timeStamp;
        
        Log.d("PalmasTour", "Timestamp gerado: " + timeStamp);

        // Cria um objeto Location com ID UUID
        com.kopecode.palmastour.model.Location location = new com.kopecode.palmastour.model.Location(
                currentLocation.latitude,
                currentLocation.longitude,
                locationName,
                timeStamp
        );
        
        // Log para debug
        Log.d("PalmasTour", "Tentando salvar localização: " + location.getId() + ", lat: " + location.getLatitude() + ", lng: " + location.getLongitude());

        // Salva a localização e as fotos em uma thread separada
        new Thread(() -> {
            try {
                // Obtém a instância do cliente Supabase
                SupabaseClient client = SupabaseClient.getInstance(requireContext());
                
                // Tenta salvar a localização
                Log.d("PalmasTour", "Enviando requisição para salvar localização");
                boolean locationSaved = client.saveLocation(location);
                Log.d("PalmasTour", "Resultado do salvamento da localização: " + locationSaved);
                
                if (locationSaved) {
                    try {
                        // Aguarda um momento para garantir que a localização foi salva no servidor
                        // Aumentando o tempo de espera para garantir que a localização seja registrada no servidor
                        Log.d("PalmasTour", "Aguardando sincronização com o servidor...");
                        Thread.sleep(3000);
                        
                        // Obtém o ID da localização após salvar
                        Log.d("PalmasTour", "Buscando localizações para encontrar a recém-salva");
                        List<Location> locations = client.getLocations();
                        Log.d("PalmasTour", "Número de localizações encontradas: " + locations.size());
                        
                        Location savedLocation = null;
                        
                        // Procura a localização recém-salva (a mais recente com as mesmas coordenadas)
                        // Aumentando a margem de erro para comparação de coordenadas
                        double errorMargin = 0.0001; // Margem de erro maior para comparação de coordenadas
                        
                        // Ordenar localizações por data de criação (mais recentes primeiro)
                        locations.sort((loc1, loc2) -> {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                                Date date1 = loc1.getCreatedAt();
                                Date date2 = loc2.getCreatedAt();
                                return date2.compareTo(date1); // Ordem decrescente (mais recente primeiro)
                            } catch (Exception e) {
                                return 0;
                            }
                        });
                        
                        for (Location loc : locations) {
                            Log.d("PalmasTour", "Verificando localização: " + loc.getId() + ", lat: " + loc.getLatitude() + ", lng: " + loc.getLongitude() + ", data: " + loc.getCreatedAt());
                            
                            // Compara as coordenadas com uma margem de erro
                            if (Math.abs(loc.getLatitude() - location.getLatitude()) < errorMargin && 
                                Math.abs(loc.getLongitude() - location.getLongitude()) < errorMargin) {
                                // Encontrou a localização salva
                                savedLocation = loc;
                                Log.d("PalmasTour", "Localização encontrada com ID: " + loc.getId());
                                break;
                            }
                        }
                        
                        if (savedLocation != null) {
                            final String locationId = savedLocation.getId();
                            Log.d("PalmasTour", "Usando locationId: " + locationId + " para salvar fotos");
                            
                            // Contador para acompanhar o progresso
                            final int[] successCount = {0};
                            final int totalPhotos = photosPaths.size();
                            
                            // Salva cada foto com tratamento de erros
                            for (String photoPath : photosPaths) {
                                try {
                                    File photoFile = new File(photoPath);
                                    if (photoFile.exists()) {
                                        Log.d("PalmasTour", "Salvando foto: " + photoPath + ", tamanho: " + photoFile.length() + " bytes");
                                        // Usar o mesmo formato de timestamp para a foto
                                        String photoTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());
                                        Log.d("PalmasTour", "Timestamp para foto: " + photoTimestamp);
                                        
                                        Photo photo = new Photo(
                                                locationId,
                                                photoPath,
                                                photoTimestamp,
                                                "Foto da localização"
                                        );
                                        
                                        Log.d("PalmasTour", "Criada foto com ID: " + photo.getId() + ", locationId: " + photo.getLocationId());
                                        boolean photoSaved = client.savePhoto(photo, photoFile);
                                        if (photoSaved) {
                                            successCount[0]++;
                                            Log.d("PalmasTour", "Foto salva com sucesso: " + photoPath);
                                        } else {
                                            Log.e("PalmasTour", "Falha ao salvar foto: " + photoPath);
                                        }
                                    } else {
                                        Log.e("PalmasTour", "Arquivo de foto não encontrado: " + photoPath);
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Arquivo de foto não encontrado: " + photoPath, Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    final String errorMsg = e.getMessage();
                                    Log.e("PalmasTour", "Erro ao salvar foto: " + errorMsg);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Erro ao salvar foto: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                            
                            // Notifica sucesso na UI thread
                            final int finalSuccessCount = successCount[0];
                            requireActivity().runOnUiThread(() -> {
                                String message = String.format("Trabalho salvo! %d/%d fotos salvas com sucesso.", finalSuccessCount, totalPhotos);
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                // Limpa os dados após salvar
                                photosPaths.clear();
                                updatePhotosCount();
                                mMap.clear();
                                homeViewModel.setIsSaving(false);
                                
                                // Ocultar os botões após salvar
                                binding.btnTakePhoto.setVisibility(View.GONE);
                                binding.btnSaveWork.setVisibility(View.GONE);
                            });
                        } else {
                            // Tenta uma abordagem alternativa: usar o ID da localização que acabamos de criar
                            Log.d("PalmasTour", "Tentando usar o ID da localização recém-criada: " + location.getId());
                            final String locationId = location.getId();
                            
                            // Contador para acompanhar o progresso
                            final int[] successCount = {0};
                            final int totalPhotos = photosPaths.size();
                            
                            // Salva cada foto com tratamento de erros
                            for (String photoPath : photosPaths) {
                                try {
                                    File photoFile = new File(photoPath);
                                    if (photoFile.exists()) {
                                        Log.d("PalmasTour", "Salvando foto com ID alternativo: " + photoPath);
                                        Photo photo = new Photo(
                                                locationId,
                                                photoPath,
                                                timeStamp,
                                                "Foto da localização"
                                        );
                                        boolean photoSaved = client.savePhoto(photo, photoFile);
                                        if (photoSaved) {
                                            successCount[0]++;
                                            Log.d("PalmasTour", "Foto salva com sucesso usando ID alternativo: " + photoPath);
                                        } else {
                                            Log.e("PalmasTour", "Falha ao salvar foto com ID alternativo: " + photoPath);
                                        }
                                    } else {
                                        Log.e("PalmasTour", "Arquivo de foto não encontrado: " + photoPath);
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Arquivo de foto não encontrado: " + photoPath, Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    final String errorMsg = e.getMessage();
                                    Log.e("PalmasTour", "Erro ao salvar foto com ID alternativo: " + errorMsg);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Erro ao salvar foto: " + errorMsg, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                            
                            // Notifica sucesso na UI thread
                            final int finalSuccessCount = successCount[0];
                            requireActivity().runOnUiThread(() -> {
                                if (finalSuccessCount > 0) {
                                    String message = String.format("Trabalho salvo! %d/%d fotos salvas com sucesso.", finalSuccessCount, totalPhotos);
                                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                    // Limpa os dados após salvar
                                    photosPaths.clear();
                                    updatePhotosCount();
                                    mMap.clear();
                                } else {
                                    Toast.makeText(getContext(), "Localização salva, mas não foi possível salvar as fotos. Tente novamente.", Toast.LENGTH_LONG).show();
                                }
                                homeViewModel.setIsSaving(false);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        final String errorMsg = e.getMessage();
                        Log.e("PalmasTour", "Erro ao recuperar localização: " + errorMsg);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Erro ao recuperar localização: " + errorMsg, Toast.LENGTH_LONG).show();
                            homeViewModel.setIsSaving(false);
                        });
                    }
                } else {
                    Log.e("PalmasTour", "Falha ao salvar localização");
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Erro ao salvar localização. Verifique sua conexão com a internet e tente novamente.", Toast.LENGTH_LONG).show();
                        homeViewModel.setIsSaving(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                final String errorMsg = e.getMessage();
                Log.e("PalmasTour", "Erro durante o salvamento: " + errorMsg);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Erro durante o salvamento: " + errorMsg, Toast.LENGTH_LONG).show();
                    homeViewModel.setIsSaving(false);
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