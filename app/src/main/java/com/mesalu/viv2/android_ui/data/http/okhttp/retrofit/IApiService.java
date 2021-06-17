package com.mesalu.viv2.android_ui.data.http.okhttp.retrofit;

import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NewPetForm;
import com.mesalu.viv2.android_ui.data.model.NodeController;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Represents a Retrofit-compatible service that our retrofit-using dao can utilize.
 */
public interface IApiService {
    @GET("pet/ids")
    Call<List<Integer>> getPetIdList();

    @GET("pet/{id}")
    Call<Pet> getPetById(@Path("id") int id);

    /**
     * @param a String representing a date time formatting via ISO 8601, indicates start of range
     * @param b String representing a date time formatting via ISO 8601, indicates end of range.
     * @return
     */
    @GET("sample/slice")
    Call<List<EnvDataSample>> getSamplesInRange(String a, String b);

    @GET("pet/{id}/samples")
    Call<List<EnvDataSample>> getSamplesInRangeForPet(@Path("id") int id,
                                                      @Query("start") String start,
                                                      @Query("end") String end);


    @GET("pet/{id}/prelim")
    Call<PreliminaryPetInfo> getPreliminaryInfo(@Path("id") int id);

    @GET("species")
    Call<List<Species>> getSpeciesInfo();

    @POST("pet/add")
    Call<Integer> addPet(@Body NewPetForm form);

    @POST("pet/{id}/migrate")
    Call<Void> migratePet(@Path("id") int id, @Query("toEnv") String environmentId);

    @GET("node/mine")
    Call<List<NodeController>> getControllers();

    @GET("environment/{id}")
    Call<Environment> getEnvironmentInfo(@Path("id") String id);

    @GET("environment")
    Call<List<Environment>> getAllEnvironments();
}
