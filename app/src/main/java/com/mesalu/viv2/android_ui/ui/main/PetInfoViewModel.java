package com.mesalu.viv2.android_ui.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mesalu.viv2.android_ui.data.PetInfoRepository;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetInfoViewModel extends CommonSignalAwareViewModel {
    // all Ids of pets associated to the user
    private final MutableLiveData<List<Integer>> petIds;

    // instances of Pet for each pet associated to user
    private final HybridCollectionLiveData<Integer, Pet> pets;

    private final MutableLiveData<List<Species>> species;

    // contains "preliminary info" objects of tracked pets.
    private final Map<Integer, MutableLiveData<PreliminaryPetInfo>> preliminaryInfo;

    private final PetInfoRepository repository;

    public PetInfoViewModel() {
        super();

        repository = PetInfoRepository.getInstance();
        pets = new HybridCollectionLiveData<>(Pet::getId);
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
     * Gets an observable live data instance that will receive updates for pets of matching id
     * setting `refresh` will request the data access layer refresh the data. The access layer
     * may or may not then request fresh information from the backend API.
     *
     * @param id pet id.
     * @param refresh should new data be requested?
     * @return a LiveData instance that will receive updates for the requested pet.
     */
    public LiveData<PreliminaryPetInfo> getPreliminaryInfoObservable(int id, boolean refresh) {
        if (!preliminaryInfo.containsKey(id)) {
            preliminaryInfo.put(id, new MutableLiveData<>());
        }

        MutableLiveData<PreliminaryPetInfo> observable = preliminaryInfo.get(id);
        if (refresh)
            repository.getPreliminaryPetInfo(id, this::extractAndUpdatePreliminaryPet);

        return observable;
    }

    /**
     * As getPreliminaryInfoObservable(id, false)
     */
    public LiveData<PreliminaryPetInfo> getPreliminaryInfoObservable(int id) {
        return getPreliminaryInfoObservable(id, false);
    }

    /**
     * Requests that the underlying data access layer refresh the PreliminaryPetInfo field
     * for the specified pet id.
     * @param petId
     */
    public void refreshPreliminaryInfoFor(int petId) {
        repository.getPreliminaryPetInfo(petId, this::extractAndUpdatePreliminaryPet);
    }

    /**
     * Ensures the pet id list is up to date and then requests a preliminary pet info update
     * for each id within.
     */
    public void refreshAllPreliminaryPetInfo() {
        repository.getPetIdList(l -> {
            petIds.setValue(l);
            for (int petId : l) {
                refreshPreliminaryInfoFor(petId);
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

    private void extractAndUpdatePreliminaryPet(PreliminaryPetInfo preliminaryPetInfo) {
        // preliminaryPetInfo.getPet() will contain the same info as would
        // land in the pet list anyways, so we'll treat it the same.
        pets.update(preliminaryPetInfo.getPet());

        int id = preliminaryPetInfo.getPet().getId();
        if (!preliminaryInfo.containsKey(id))
            preliminaryInfo.put(id, new MutableLiveData<>());

        preliminaryInfo.get(id).setValue(preliminaryPetInfo);
    }
}