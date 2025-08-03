package com.kopecode.palmastour.ui.slideshow;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kopecode.palmastour.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class SlideshowViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mLocationId;
    private final MutableLiveData<String> mLocationName;
    private final MutableLiveData<List<Photo>> mPhotos;
    private final MutableLiveData<Boolean> mIsLoading;

    public SlideshowViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Fotos da localização");
        
        mLocationId = new MutableLiveData<>();
        mLocationName = new MutableLiveData<>();
        mPhotos = new MutableLiveData<>(new ArrayList<>());
        mIsLoading = new MutableLiveData<>(false);
    }

    public LiveData<String> getText() {
        return mText;
    }
    
    public LiveData<String> getLocationId() {
        return mLocationId;
    }
    
    public void setLocationId(String locationId) {
        mLocationId.setValue(locationId);
    }
    
    public LiveData<String> getLocationName() {
        return mLocationName;
    }
    
    public void setLocationName(String locationName) {
        mLocationName.setValue(locationName);
    }
    
    public LiveData<List<Photo>> getPhotos() {
        return mPhotos;
    }
    
    public void setPhotos(List<Photo> photos) {
        mPhotos.setValue(photos);
    }
    
    public LiveData<Boolean> getIsLoading() {
        return mIsLoading;
    }
    
    public void setIsLoading(boolean isLoading) {
        mIsLoading.setValue(isLoading);
    }
}