package com.mesalu.viv2.android_ui.ui.overview;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.PetInfoRepository;
import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetReviewViewModel extends ViewModel {
    // all Ids of pets associated to the user
    private MutableLiveData<List<Integer>> petIds;

    // instances of Pet for each pet associated to user
    private MutableLiveData<List<Pet>> pets;

    // contains all samples from the past week
    private MutableLiveData<List<EnvDataSample>> thisWeeksSamples;

    private Map<Integer, MutableLiveData<PreliminaryPetInfo>> preliminaryInfo;

    private PetInfoRepository repository;

    public PetReviewViewModel() {
        repository = PetInfoRepository.getInstance();
        pets = new MutableLiveData<>();
        petIds = new MutableLiveData<>();
        preliminaryInfo = new HashMap<>();
    }

    /**
     * Indicates if the pet id list has been successfully loaded
     * @return
     */
    public boolean petIdListLoaded() {
        return petIds.getValue() != null;
    }

    /**
     * Dispatches an action to request data from the pertinent data source.
     * Upon completion an update will be observed on observers registered with
     * `observePetsList`
     */
    public void fetchPetIdList() {
        repository.getPetIdList(ids -> petIds.setValue(ids));
    }

    public LiveData<List<Integer>> getIdObservable() {
        return petIds;
    }

    /**
     * Gets the preliminary info for the specified pet. If not available a request is dispatched
     * to get the info before invoking the observer's callback. In either case the callback
     * is invoked on the calling thread.
     *
     * If an error occurs - such as login state being invalid, or the specified pet not existing -
     * then the callback is never invoked.
     *
     * @param id The ID of the pet to get info for.
     * @param owner Lifecycle owner of the observer.
     * @param observer Callback for when data is ready
     */
    public void getPreliminaryPetInfo(int id,
                                      LifecycleOwner owner, Observer<PreliminaryPetInfo> observer) {

        if (preliminaryInfo.containsKey(id)) {
            // can just attach directly. My understanding is that this
            // immediately invokes the required callback.
            preliminaryInfo.get(id).observe(owner, observer);
        }
        else {
            final MutableLiveData<PreliminaryPetInfo> pendingData = new MutableLiveData<>();
            preliminaryInfo.put(id, pendingData);

            // register the observer:
            pendingData.observe(owner, observer);

            // kick off a request to fill that data item.
            repository.getPreliminaryPetInfo(id,
                    pendingData::setValue);
        }
    }

    public LiveData<List<Pet>> getPetListObservable() {
        return pets;
    }
}