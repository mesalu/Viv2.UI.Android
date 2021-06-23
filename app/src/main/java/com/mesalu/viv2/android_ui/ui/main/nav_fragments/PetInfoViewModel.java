package com.mesalu.viv2.android_ui.ui.main.nav_fragments;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mesalu.viv2.android_ui.data.PetInfoRepository;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;
import com.mesalu.viv2.android_ui.ui.main.CommonSignalAwareViewModel;
import com.mesalu.viv2.android_ui.ui.main.HybridCollectionLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetInfoViewModel extends CommonSignalAwareViewModel {
    // all Ids of pets associated to the user
    private final MutableLiveData<List<Integer>> petIds;

    // instances of Pet for each pet associated to user
    private final HybridCollectionLiveData<Integer, Pet> pets;

    private final MutableLiveData<List<Species>> species;

    private final PetInfoRepository repository;

    public PetInfoViewModel() {
        super();

        repository = PetInfoRepository.getInstance();
        pets = new HybridCollectionLiveData<>(Pet::getId);
        species = new MutableLiveData<>();
        petIds = new MutableLiveData<>();
    }

    @Override
    public void clearUserSensitiveData() {
        pets.clear();
        species.setValue(null);
        petIds.setValue(null);
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
     * Ensures the pet id list is up to date and then requests extended information
     * for each id within.
     */
    public void refreshAllPetInfo() {
        repository.getPetIdList(l -> {
            petIds.setValue(l);
            for (int petId : l) {
                updatePetById(petId);
            }
        });
    }

    /**
     * Gets a LiveData instance that will receive updates for a pet with matching ID.
     * @param id the id of the pet-of-interest.
     * @param requestUpdate calls updatePetById(id) if true.
     * @return
     */
    public LiveData<Pet> getPetObservable(int id, boolean requestUpdate) {
        if (requestUpdate) updatePetById(id);
        return pets.getObservableForId(id);
    }

    /**
     * As getPetObservable(int, bool), but sets requestUpdate to true by default.
     * @param id id of Pet to get an observable for.
     * @return a LiveData instance suitable for getting updates on the specific pet.
     */
    public LiveData<Pet> getPetObservable(int id) {
        return getPetObservable(id, true);
    }

    /**
     * Requests underlying layers to fetch updated information on the specified pet ID.
     * Fulfillment and timing of fulfillment are dependent on those underlying layers.
     */
    public void updatePetById(int id) {
        repository.getPetInfo(id, pets::update);
    }

    /**
     * Returns an observable that receives a list of populated pet objects as they're
     * acquired. This is different from petIds as petIds is ensured to have an id per-pet
     * that the active user may be interested in, whereas this list only contains pets that
     * have been loaded.
     * Note that the lists posted to this live data are unmodifiable.
     * @return live data suitable for observing list-of-pets updates
     */
    public LiveData<List<Pet>> getPetListObservable() {
        return pets.getListObservable();
    }

    public void submitNewPet(Pet pet) {
        // pass down to Repo, request repo update.
        repository.addPetAndUpdateAll(pet, ids -> petIds.setValue(ids));
    }

    private void fetchSpeciesList() {
        PetInfoRepository.getInstance().getSpeciesInfo(s -> species.setValue(s));
    }
}