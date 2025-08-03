package com.kopecode.palmastour.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Photo implements Serializable {
    @Expose
    private String id;
    
    @SerializedName("location_id")
    @Expose
    private String locationId;
    
    @SerializedName("file_path")
    @Expose
    private String filePath;
    
    @SerializedName("storage_url")
    @Expose
    private String storageUrl;
    
    @SerializedName("created_at")
    @Expose
    private Date createdAt;
    
    @Expose
    private String description;

    public Photo() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
    }

    public Photo(String locationId, String filePath) {
        this();
        this.locationId = locationId;
        this.filePath = filePath;
        this.description = "Foto tirada em " + new Date().toString();
    }
    
    public Photo(String locationId, String filePath, String timestamp, String description) {
        this();
        this.locationId = locationId;
        this.filePath = filePath;
        this.description = description;
        
        android.util.Log.d("PalmasTour", "Criando foto com locationId: " + locationId);
        android.util.Log.d("PalmasTour", "Foto ID gerado: " + this.id);
        
        // Se timestamp não for nulo, tenta converter para Date
        if (timestamp != null && !timestamp.isEmpty()) {
            try {
                android.util.Log.d("PalmasTour", "Tentando converter timestamp: " + timestamp);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                this.createdAt = dateFormat.parse(timestamp);
                android.util.Log.d("PalmasTour", "Timestamp convertido para Date: " + this.createdAt);
            } catch (ParseException e) {
                e.printStackTrace();
                android.util.Log.e("PalmasTour", "Erro ao converter timestamp: " + e.getMessage());
                // Mantém a data atual se não conseguir converter
                android.util.Log.d("PalmasTour", "Usando data atual: " + this.createdAt);
            }
        } else {
            android.util.Log.d("PalmasTour", "Timestamp nulo ou vazio, usando data atual: " + this.createdAt);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}