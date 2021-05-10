package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.mesalu.viv2.android_ui.data.PetInfoRepository;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;
import com.mesalu.viv2.android_ui.ui.events.ConsumableEvent;
import com.mesalu.viv2.android_ui.ui.events.SimpleEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetInfoViewModel extends FabAwareViewModel {
    // all Ids of pets associated to the user
    private MutableLiveData<List<Integer>> petIds;

    // instances of Pet for each pet associated to user
    private Map<Integer, MutableLiveData<Pet>> pets;

    private MutableLiveData<List<Species>> species;

    // contains all samples from the past week
    private MutableLiveData<List<EnvDataSample>> thisWeeksSamples;

    // contains "preliminary info" objects of tracked pets.
    private Map<Integer, MutableLiveData<PreliminaryPetInfo>> preliminaryInfo;

    private PetInfoRepository repository;

    public PetInfoViewModel() {
        super();

        repository = PetInfoRepository.getInstance();
        pets = new HashMap<>();
        species = new MutableLiveData<>();
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

    public LiveData<List<Species>> getSpeciesObservable() {
        if (species.getValue() == null) {
            // TODO: or if its particularly out of date.
            fetchSpeciesList();
        }

        return species;
    }

    public LiveData<List<Integer>> getIdsObservable() {
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

    public LiveData<Pet> getPetObservable(int id) {
        if (!pets.containsKey(id)) {
            final MutableLiveData<Pet> petObservable = new MutableLiveData<>();
            pets.put(id, petObservable);
            // request data load (Depend on repo for being aware enough to merge prelim & pet)
            repository.getPetInfo(id, petObservable::setValue);
        }
        return pets.get(id);
    }

    public void submitNewPet(Pet pet) {
        // pass down to Repo, request repo update.
        repository.addPetAndUpdateAll(pet, ids -> petIds.setValue(ids));
    }

    private void fetchSpeciesList() {
        PetInfoRepository.getInstance().getSpeciesInfo(s -> species.setValue(s));
    }
}