package com.mesalu.viv2.android_ui.data.http.retrofit;

import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.NewPetForm;
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

/**
 * Represents a Retrofit-compatible service that our retrofit-using dao can utilize.
 */
public interface IApiService {
    @GET("pet/ids")
    Call<List<Integer>> getPetIdList(@HeaderMap Map<String, String> headers);

    @GET("pet/{id}")
    Call<Pet> getPetById(@HeaderMap Map<String, String> headers, @Path("id") int id);

    /**
     * @param headers
     * @param a String representing a date time formatting via ISO 8601, indicates start of range
     * @param b String representing a date time formatting via ISO 8601, indicates end of range.
     * @return
     */
    @GET("sample/slice")
    Call<List<EnvDataSample>> getSamplesInRange(@HeaderMap Map<String, String> headers, String a, String b);


    @GET("pet/{id}/prelim")
    Call<PreliminaryPetInfo> getPreliminaryInfo(@HeaderMap Map<String, String> headers, @Path("id") int id);

    @GET("species")
    Call<List<Species>> getSpeciesInfo(@HeaderMap Map<String, String> headers);

    @POST("pet/add")
    Call<Integer> addPet(@HeaderMap Map<String, String> headers, @Body NewPetForm form);
}
