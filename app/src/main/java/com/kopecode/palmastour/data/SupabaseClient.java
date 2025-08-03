package com.kopecode.palmastour.data;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kopecode.palmastour.R;
import com.kopecode.palmastour.model.Location;
import com.kopecode.palmastour.model.Photo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {
    private static SupabaseClient instance;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final OkHttpClient client;
    private final Gson gson;
    private final Context context;

    private SupabaseClient(Context context) {
        this.context = context.getApplicationContext(); // Usar o contexto da aplicação para evitar memory leaks
        this.supabaseUrl = context.getString(R.string.supabase_url);
        this.supabaseKey = context.getString(R.string.supabase_key);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context);
        }
        return instance;
    }

    // Métodos para Locations
    public boolean saveLocation(Location location) {
        try {
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return false;
            }
            
            // Verificar se a URL do Supabase é válida
            if (supabaseUrl == null || supabaseUrl.isEmpty()) {
                android.util.Log.e("PalmasTour", "URL do Supabase é nula ou vazia");
                return false;
            }
            
            // Verificar se a chave do Supabase é válida
            if (supabaseKey == null || supabaseKey.isEmpty()) {
                android.util.Log.e("PalmasTour", "Chave do Supabase é nula ou vazia");
                return false;
            }
            
            // Verificar se o ID é um UUID válido
            try {
                UUID uuid = UUID.fromString(location.getId());
                android.util.Log.d("PalmasTour", "ID da localização é um UUID válido: " + uuid);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "ID da localização não é um UUID válido: " + location.getId());
                // Gerar um novo UUID válido
                String newId = UUID.randomUUID().toString();
                android.util.Log.d("PalmasTour", "Substituindo ID por um UUID válido: " + newId);
                location.setId(newId);
            }
            
            String json = gson.toJson(location);
            android.util.Log.d("PalmasTour", "JSON para envio: " + json);
            android.util.Log.d("PalmasTour", "Supabase URL: " + supabaseUrl);
            android.util.Log.d("PalmasTour", "Supabase Key (truncado): " + (supabaseKey.length() > 10 ? supabaseKey.substring(0, 10) + "..." : supabaseKey));
            
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

            String requestUrl = supabaseUrl + "/rest/v1/locations";
            android.util.Log.d("PalmasTour", "URL completa da requisição: " + requestUrl);
            
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para: " + request.url());
            android.util.Log.d("PalmasTour", "Headers da requisição: " + request.headers());
            
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Localização salva com sucesso: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao salvar localização: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao salvar localização: " + e.getMessage());
            android.util.Log.e("PalmasTour", "Stack trace: " + android.util.Log.getStackTraceString(e));
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao salvar localização: " + e.getMessage());
            android.util.Log.e("PalmasTour", "Stack trace: " + android.util.Log.getStackTraceString(e));
            return false;
        }
    }

    public List<Location> getLocations() {
        try {
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/locations?select=*")
                    .get()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                Location[] locations = gson.fromJson(json, Location[].class);
                List<Location> locationList = new ArrayList<>();
                for (Location location : locations) {
                    // Carregar as fotos para cada localização
                    List<Photo> photos = getPhotosByLocation(location.getId());
                    location.setPhotos(photos);
                    locationList.add(location);
                }
                return locationList;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public Location getLocation(String locationId) {
        try {
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/locations?id=eq." + locationId)
                    .get()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                Location[] locations = gson.fromJson(json, Location[].class);
                if (locations.length > 0) {
                    Location location = locations[0];
                    // Carregar as fotos para esta localização
                    location.setPhotos(getPhotosByLocation(locationId));
                    return location;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean updateLocation(Location location) {
        try {
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return false;
            }
            
            // Verificar se o ID é um UUID válido
            try {
                UUID uuid = UUID.fromString(location.getId());
                android.util.Log.d("PalmasTour", "ID da localização é um UUID válido: " + uuid);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "ID da localização não é um UUID válido: " + location.getId());
                return false;
            }
            
            String json = gson.toJson(location);
            android.util.Log.d("PalmasTour", "JSON para atualização: " + json);
            
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

            String requestUrl = supabaseUrl + "/rest/v1/locations?id=eq." + location.getId();
            android.util.Log.d("PalmasTour", "URL completa da requisição: " + requestUrl);
            
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .patch(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para: " + request.url());
            
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Localização atualizada com sucesso: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao atualizar localização: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao atualizar localização: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao atualizar localização: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteLocation(String locationId) {
        try {
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return false;
            }
            
            // Verificar se o ID é um UUID válido
            try {
                UUID uuid = UUID.fromString(locationId);
                android.util.Log.d("PalmasTour", "ID da localização é um UUID válido: " + uuid);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "ID da localização não é um UUID válido: " + locationId);
                return false;
            }
            
            // Primeiro, obter todas as fotos associadas a esta localização e excluí-las
             List<Photo> photos = getPhotosByLocation(locationId);
             if (photos != null && !photos.isEmpty()) {
                 android.util.Log.d("PalmasTour", "Excluindo " + photos.size() + " fotos associadas à localização");
                 
                 for (Photo photo : photos) {
                     boolean photoDeleted = deletePhoto(photo.getId());
                     android.util.Log.d("PalmasTour", "Foto " + photo.getId() + " excluída: " + photoDeleted);
                 }
             } else {
                 android.util.Log.d("PalmasTour", "Nenhuma foto associada à localização para excluir");
             }
            
            // Agora excluir a localização
            String requestUrl = supabaseUrl + "/rest/v1/locations?id=eq." + locationId;
            android.util.Log.d("PalmasTour", "URL completa da requisição de exclusão: " + requestUrl);
            
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .delete()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para: " + request.url());
            
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Localização excluída com sucesso: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao excluir localização: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao excluir localização: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao excluir localização: " + e.getMessage());
            return false;
        }
    }

    // Métodos para Photos
    public boolean savePhoto(Photo photo) {
        try {
            // Verificar se o ID da foto é um UUID válido
            if (photo.getId() == null || photo.getId().isEmpty()) {
                photo.setId(UUID.randomUUID().toString());
                android.util.Log.d("PalmasTour", "Gerando novo ID para a foto: " + photo.getId());
            } else {
                try {
                    UUID.fromString(photo.getId());
                } catch (IllegalArgumentException e) {
                    String newId = UUID.randomUUID().toString();
                    android.util.Log.d("PalmasTour", "Substituindo ID da foto por um UUID válido: " + newId);
                    photo.setId(newId);
                }
            }
            
            // Verificar se o locationId é um UUID válido
            if (photo.getLocationId() != null && !photo.getLocationId().isEmpty()) {
                try {
                    UUID.fromString(photo.getLocationId());
                    android.util.Log.d("PalmasTour", "LocationId da foto é um UUID válido: " + photo.getLocationId());
                } catch (IllegalArgumentException e) {
                    android.util.Log.e("PalmasTour", "LocationId da foto não é um UUID válido: " + photo.getLocationId());
                    return false; // Não podemos salvar a foto sem um locationId válido
                }
            } else {
                android.util.Log.e("PalmasTour", "LocationId da foto é nulo ou vazio");
                return false; // Não podemos salvar a foto sem um locationId
            }
            
            String json = gson.toJson(photo);
            android.util.Log.d("PalmasTour", "JSON para envio de foto: " + json);
            
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/photos")
                    .post(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para salvar foto: " + request.url());
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Foto salva com sucesso: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao salvar foto: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao salvar foto: " + e.getMessage());
            return false;
        }
    }
    
    public boolean savePhoto(Photo photo, File photoFile) {
        try {
            android.util.Log.d("PalmasTour", "Iniciando salvamento de foto com arquivo: " + (photoFile != null ? photoFile.getName() : "null"));
            
            if (photoFile == null) {
                android.util.Log.e("PalmasTour", "Arquivo de foto é nulo");
                return false;
            }
            
            if (!photoFile.exists()) {
                android.util.Log.e("PalmasTour", "Arquivo de foto não existe: " + photoFile.getAbsolutePath());
                return false;
            }
            
            if (!photoFile.canRead()) {
                android.util.Log.e("PalmasTour", "Não é possível ler o arquivo de foto: " + photoFile.getAbsolutePath());
                return false;
            }
            
            android.util.Log.d("PalmasTour", "Tamanho do arquivo: " + photoFile.length() + " bytes");
            
            // Verificar se o locationId é um UUID válido
            if (photo.getLocationId() == null || photo.getLocationId().isEmpty()) {
                android.util.Log.e("PalmasTour", "LocationId da foto é nulo ou vazio");
                return false;
            }
            
            try {
                UUID.fromString(photo.getLocationId());
                android.util.Log.d("PalmasTour", "LocationId da foto é um UUID válido: " + photo.getLocationId());
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "LocationId da foto não é um UUID válido: " + photo.getLocationId());
                return false;
            }
            
            // Primeiro, faz upload do arquivo para o storage
            String photoUrl = uploadFile(photoFile, "photos", photo.getLocationId() + "/" + photoFile.getName());
            android.util.Log.d("PalmasTour", "Resultado do upload: " + (photoUrl != null ? "sucesso" : "falha"));
            
            // Se o upload foi bem-sucedido, atualiza o caminho da foto e salva no banco
            if (photoUrl != null) {
                photo.setStorageUrl(photoUrl);
                android.util.Log.d("PalmasTour", "URL de armazenamento definida: " + photoUrl);
                boolean result = savePhoto(photo);
                android.util.Log.d("PalmasTour", "Resultado do salvamento dos metadados da foto: " + (result ? "sucesso" : "falha"));
                return result;
            }
            
            android.util.Log.e("PalmasTour", "Falha ao fazer upload do arquivo de foto");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao salvar foto com arquivo: " + e.getMessage());
            android.util.Log.e("PalmasTour", "Stack trace: " + android.util.Log.getStackTraceString(e));
            return false;
        }
    }

    public List<Photo> getPhotosByLocation(String locationId) {
        try {
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/rest/v1/photos?location_id=eq." + locationId)
                    .get()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                Photo[] photos = gson.fromJson(json, Photo[].class);
                List<Photo> photoList = new ArrayList<>();
                for (Photo photo : photos) {
                    photoList.add(photo);
                }
                return photoList;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    
    public boolean updatePhoto(Photo photo) {
        try {
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return false;
            }
            
            // Verificar se o ID da foto é um UUID válido
            try {
                UUID uuid = UUID.fromString(photo.getId());
                android.util.Log.d("PalmasTour", "ID da foto é um UUID válido: " + uuid);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "ID da foto não é um UUID válido: " + photo.getId());
                return false;
            }
            
            String json = gson.toJson(photo);
            android.util.Log.d("PalmasTour", "JSON para atualização da foto: " + json);
            
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);

            String requestUrl = supabaseUrl + "/rest/v1/photos?id=eq." + photo.getId();
            android.util.Log.d("PalmasTour", "URL completa da requisição: " + requestUrl);
            
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .patch(body)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para: " + request.url());
            
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Foto atualizada com sucesso: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao atualizar foto: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao atualizar foto: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao atualizar foto: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deletePhoto(String photoId) {
        try {
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return false;
            }
            
            // Verificar se o ID é um UUID válido
            try {
                UUID uuid = UUID.fromString(photoId);
                android.util.Log.d("PalmasTour", "ID da foto é um UUID válido: " + uuid);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "ID da foto não é um UUID válido: " + photoId);
                return false;
            }
            
            // Primeiro, obter a foto para conseguir a URL do storage
            Photo photo = getPhotoById(photoId);
            if (photo == null) {
                android.util.Log.e("PalmasTour", "Não foi possível obter a foto para exclusão: " + photoId);
                return false;
            }
            
            // Extrair o caminho do arquivo no storage a partir da URL
            String storageUrl = photo.getStorageUrl();
            if (storageUrl != null && !storageUrl.isEmpty()) {
                String storagePath = extractStoragePathFromUrl(storageUrl);
                if (storagePath != null) {
                    // Excluir o arquivo do storage
                    boolean storageDeleted = deleteFileFromStorage(storagePath);
                    if (!storageDeleted) {
                        android.util.Log.e("PalmasTour", "Não foi possível excluir o arquivo do storage: " + storagePath);
                        // Continuar mesmo se a exclusão do storage falhar
                    } else {
                        android.util.Log.d("PalmasTour", "Arquivo excluído com sucesso do storage: " + storagePath);
                    }
                } else {
                    android.util.Log.e("PalmasTour", "Não foi possível extrair o caminho do storage da URL: " + storageUrl);
                }
            } else {
                android.util.Log.d("PalmasTour", "Foto não possui URL de storage para exclusão");
            }
            
            // Agora excluir o registro da foto no banco de dados
            String requestUrl = supabaseUrl + "/rest/v1/photos?id=eq." + photoId;
            android.util.Log.d("PalmasTour", "URL completa da requisição: " + requestUrl);
            
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .delete()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para: " + request.url());
            
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Foto excluída com sucesso: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao excluir foto: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao excluir foto: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao excluir foto: " + e.getMessage());
            return false;
        }
    }

    // Método para upload de arquivos
    public String uploadFile(File file, String bucket, String path) {
        try {
            android.util.Log.d("PalmasTour", "Iniciando upload de arquivo: " + file.getName() + ", tamanho: " + file.length() + " bytes");
            android.util.Log.d("PalmasTour", "Bucket: " + bucket + ", Path: " + path);
            
            if (!file.exists()) {
                android.util.Log.e("PalmasTour", "Arquivo não existe: " + file.getAbsolutePath());
                return null;
            }
            
            if (!file.canRead()) {
                android.util.Log.e("PalmasTour", "Não é possível ler o arquivo: " + file.getAbsolutePath());
                return null;
            }
            
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();

            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
            android.util.Log.d("PalmasTour", "URL de upload: " + uploadUrl);
            
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição de upload");
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
                android.util.Log.d("PalmasTour", "Upload bem-sucedido. URL pública: " + publicUrl);
                return publicUrl;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro no upload: " + response.code() + ", Corpo: " + responseBody);
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao fazer upload do arquivo: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Obtém uma foto pelo seu ID
     * @param photoId ID da foto a ser obtida
     * @return Objeto Photo ou null se não encontrado
     */
    public Photo getPhotoById(String photoId) {
        try {
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return null;
            }
            
            // Verificar se o ID é um UUID válido
            try {
                UUID uuid = UUID.fromString(photoId);
                android.util.Log.d("PalmasTour", "ID da foto é um UUID válido: " + uuid);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PalmasTour", "ID da foto não é um UUID válido: " + photoId);
                return null;
            }
            
            String requestUrl = supabaseUrl + "/rest/v1/photos?id=eq." + photoId;
            android.util.Log.d("PalmasTour", "URL completa da requisição: " + requestUrl);
            
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .get()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição para: " + request.url());
            
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "[]";
                android.util.Log.d("PalmasTour", "Resposta: " + responseBody);
                
                if (responseBody.equals("[]") || responseBody.isEmpty()) {
                    android.util.Log.d("PalmasTour", "Nenhuma foto encontrada com o ID: " + photoId);
                    return null;
                }
                
                // Converter o JSON para um array de fotos
                Photo[] photos = gson.fromJson(responseBody, Photo[].class);
                
                if (photos != null && photos.length > 0) {
                    android.util.Log.d("PalmasTour", "Foto encontrada: " + photos[0].getId());
                    return photos[0];
                } else {
                    android.util.Log.d("PalmasTour", "Nenhuma foto encontrada após conversão JSON");
                    return null;
                }
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao obter foto: " + response.code() + ", Corpo: " + responseBody);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao obter foto: " + e.getMessage());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao obter foto: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extrai o caminho do arquivo no storage a partir da URL pública
     * @param storageUrl URL pública do arquivo no storage
     * @return Caminho do arquivo no formato bucket/path ou null se não for possível extrair
     */
    private String extractStoragePathFromUrl(String storageUrl) {
        try {
            if (storageUrl == null || storageUrl.isEmpty()) {
                return null;
            }
            
            // A URL pública tem o formato: supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path
            String publicPrefix = supabaseUrl + "/storage/v1/object/public/";
            
            if (storageUrl.startsWith(publicPrefix)) {
                String path = storageUrl.substring(publicPrefix.length());
                android.util.Log.d("PalmasTour", "Caminho extraído da URL: " + path);
                return path;
            } else {
                android.util.Log.e("PalmasTour", "URL não tem o formato esperado: " + storageUrl);
                return null;
            }
        } catch (Exception e) {
            android.util.Log.e("PalmasTour", "Erro ao extrair caminho da URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Exclui um arquivo do storage do Supabase
     * @param path Caminho do arquivo no formato bucket/path
     * @return true se a exclusão foi bem-sucedida, false caso contrário
     */
    public boolean deleteFileFromStorage(String path) {
        try {
            android.util.Log.d("PalmasTour", "Iniciando exclusão de arquivo no storage: " + path);
            
            // Verificar conectividade de rede
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                    context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            if (!isConnected) {
                android.util.Log.e("PalmasTour", "Sem conexão com a internet");
                return false;
            }
            
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + path;
            android.util.Log.d("PalmasTour", "URL de exclusão: " + deleteUrl);
            
            Request request = new Request.Builder()
                    .url(deleteUrl)
                    .delete()
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            android.util.Log.d("PalmasTour", "Enviando requisição de exclusão");
            Response response = client.newCall(request).execute();
            
            if (response.isSuccessful()) {
                android.util.Log.d("PalmasTour", "Arquivo excluído com sucesso do storage: " + response.code());
                return true;
            } else {
                String responseBody = response.body() != null ? response.body().string() : "Sem corpo na resposta";
                android.util.Log.e("PalmasTour", "Erro ao excluir arquivo do storage: " + response.code() + ", Corpo: " + responseBody);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção ao excluir arquivo do storage: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Exceção inesperada ao excluir arquivo do storage: " + e.getMessage());
            return false;
        }
    }
}