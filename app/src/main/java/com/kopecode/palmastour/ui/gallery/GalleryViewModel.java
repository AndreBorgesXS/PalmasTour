package com.kopecode.palmastour.ui.gallery;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kopecode.palmastour.model.Location;

import java.util.ArrayList;
import java.util.List;

public class GalleryViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<List<Location>> mLocations;
    private final MutableLiveData<Boolean> mIsLoading;

    public GalleryViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Localizações");
        
        mLocations = new MutableLiveData<>(new ArrayList<>());
        mIsLoading = new MutableLiveData<>(false);
    }

    public LiveData<String> getText() {
        return mText;
    }
    
    public LiveData<List<Location>> getLocations() {
        return mLocations;
    }
    
    public void setLocations(List<Location> locations) {
        mLocations.setValue(locations);
    }
    
    public LiveData<Boolean> getIsLoading() {
        return mIsLoading;
    }
    
    public void setIsLoading(boolean isLoading) {
        mIsLoading.setValue(isLoading);
    }
}