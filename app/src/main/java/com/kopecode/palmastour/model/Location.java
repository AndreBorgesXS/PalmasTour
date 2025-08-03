package com.kopecode.palmastour.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Location implements Serializable {
    @Expose
    private String id;
    @Expose
    private double latitude;
    @Expose
    private double longitude;
    @Expose
    private String name;
    
    @SerializedName("created_at")
    @Expose
    private Date createdAt;
    
    @Expose(serialize = false)
    private transient List<Photo> photos;

    public Location() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.photos = new ArrayList<>();
    }

    public Location(double latitude, double longitude) {
        this();
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = "Localização em " + new Date().toString();
    }
    
    public Location(double latitude, double longitude, String name, String timestamp) {
        this();
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        try {
            // Tenta converter o timestamp para Date se necessário
            if (timestamp != null && !timestamp.isEmpty()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                this.createdAt = sdf.parse(timestamp);
                android.util.Log.d("PalmasTour", "Timestamp convertido para Date: " + this.createdAt);
            }
        } catch (java.text.ParseException e) {
            // Em caso de erro, mantém a data atual
            e.printStackTrace();
            android.util.Log.e("PalmasTour", "Erro ao converter timestamp: " + e.getMessage());
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
    
    public String getCreatedAtString() {
        if (createdAt != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
            return sdf.format(createdAt);
        }
        return "";
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public void addPhoto(Photo photo) {
        this.photos.add(photo);
    }

    @Override
    public String toString() {
        return name + " (" + latitude + ", " + longitude + ")";
    }
}