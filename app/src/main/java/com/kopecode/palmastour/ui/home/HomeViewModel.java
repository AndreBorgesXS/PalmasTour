package com.kopecode.palmastour.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;
import com.kopecode.palmastour.model.Location;
import com.kopecode.palmastour.model.Photo;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<LatLng> mCurrentLocation;
    private final MutableLiveData<List<String>> mPhotosPaths;
    private final MutableLiveData<Boolean> mIsSaving;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Capturar localização e fotos");
        
        mCurrentLocation = new MutableLiveData<>();
        mPhotosPaths = new MutableLiveData<>(new ArrayList<>());
        mIsSaving = new MutableLiveData<>(false);
    }

    public LiveData<String> getText() {
        return mText;
    }
    
    public LiveData<LatLng> getCurrentLocation() {
        return mCurrentLocation;
    }
    
    public void setCurrentLocation(LatLng location) {
        mCurrentLocation.setValue(location);
    }
    
    public LiveData<List<String>> getPhotosPaths() {
        return mPhotosPaths;
    }
    
    public void addPhotoPath(String path) {
        List<String> currentPaths = mPhotosPaths.getValue();
        if (currentPaths != null) {
            currentPaths.add(path);
            mPhotosPaths.setValue(currentPaths);
        }
    }
    
    public void clearPhotosPaths() {
        mPhotosPaths.setValue(new ArrayList<>());
    }
    
    public LiveData<Boolean> getIsSaving() {
        return mIsSaving;
    }
    
    public void setIsSaving(boolean isSaving) {
        mIsSaving.setValue(isSaving);
    }
}