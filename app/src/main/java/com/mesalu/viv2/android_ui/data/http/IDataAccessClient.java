package com.mesalu.viv2.android_ui.data.http;

import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * An abstraction representing actions that the rest of the app will need
 * to take in order to access data
 */
public interface IDataAccessClient {
    void getPetIdList(Consumer<List<Integer>> callback);
    void getPet(int id, Consumer<Pet> callback);
    void getPreliminaryPetInfo(int id, Consumer<PreliminaryPetInfo> callback);
    void getSamplesInDateRange(Date a, Date b, Consumer<List<EnvDataSample>> callback);
    void getSpeciesList(Consumer<List<Species>> callback);
    void addPet(Pet pet, Consumer<Pet> callback);
}
