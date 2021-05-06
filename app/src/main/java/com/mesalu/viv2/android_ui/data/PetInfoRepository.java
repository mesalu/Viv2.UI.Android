package com.mesalu.viv2.android_ui.data;

import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.http.IDataAccessClient;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;

import java.util.List;
import java.util.function.Consumer;

public class PetInfoRepository {
    private static volatile PetInfoRepository instance;
    private IDataAccessClient accessClient;

    private PetInfoRepository(IDataAccessClient accessClient) {
        this.accessClient = accessClient;
    }

    public static PetInfoRepository getInstance() {
        if (instance == null) instance = new PetInfoRepository(ClientFactory.getDataClient());
        return instance;
    }

    /**
     * Dispatches a request for a pet-id-list for the currently authenticated user.
     * This request may be fulfilled by cached data, the API, or feasibly both.
     *
     * NOTE: it is strictly possible that `consumer` be invoked twice (first with cached data then
     * with fresh data)
     *
     * @param callback
     */
    public void getPetIdList(Consumer<List<Integer>> callback) {
        accessClient.getPetIdList(callback);
    }

    /**
     * Dispatches a request for the preliminary 'at-a-glance' ifno about the specified pet.
     * If all goes well, callback.accept will be called with the data.
     *
     * The data may be serviced by internal cache, from a fresh request, or both (internal cache
     * while awaiting a fresh copy), so the callback should be written to be tolerant of multiple
     * invocations.
     *
     * @param petId the id of the pet to get info for.
     * @param callback a consumer to utilize when data is available.
     */
    public void getPreliminaryPetInfo(int petId, Consumer<PreliminaryPetInfo> callback) {
        accessClient.getPreliminaryPetInfo(petId, callback);
    }

}
