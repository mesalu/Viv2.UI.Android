package com.mesalu.viv2.android_ui.data.http.okhttp.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import com.mesalu.viv2.android_ui.data.Result;
import com.mesalu.viv2.android_ui.data.http.IDataAccessClient;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NewPetForm;
import com.mesalu.viv2.android_ui.data.model.NodeController;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;
import com.mesalu.viv2.android_ui.data.http.okhttp.OkHttpClientFactory;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataAccessClient implements IDataAccessClient {
    private final IApiService _clientService;

    public DataAccessClient() {
        _clientService = OkHttpClientFactory.getInstance()
                .buildRetrofit()
                .create(IApiService.class);
    }

    @Override
    public void getPetIdList(@NonNull Consumer<List<Integer>> callback) {
        new Retrier<List<Integer>>()
                .withCall(_clientService.getPetIdList())
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getPet(int id, Consumer<Pet> callback) {
        new Retrier<Pet>()
                .withCall(_clientService.getPetById(id))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getSamplesInDateRange(Date a, Date b, Consumer<List<EnvDataSample>> callback) {
        String start = DateTimeFormatter.ISO_INSTANT.format(a.toInstant());
        String end = DateTimeFormatter.ISO_INSTANT.format(b.toInstant());

        Call<List<EnvDataSample>> call = _clientService
                .getSamplesInRange(start, end);

        new Retrier<List<EnvDataSample>>()
                .withCall(call)
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getPreliminaryPetInfo(int id, Consumer<PreliminaryPetInfo> callback) {
        Call<PreliminaryPetInfo> call = _clientService
                .getPreliminaryInfo(id);

        new Retrier<PreliminaryPetInfo>()
                .withCall(call)
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getSpeciesList(Consumer<List<Species>> callback) {
        new Retrier<List<Species>>()
                .withCall(_clientService.getSpeciesInfo())
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void addPet(final Pet pet, Consumer<Pet> callback) {
        // convert to "NewPetForm" dto.
        NewPetForm form = new NewPetForm();
        form.setName(pet.getName());
        form.setMorph(pet.getMorph());
        form.setSpeciesId(pet.getSpecies().getId());

        new Retrier<Integer>()
                .withCall(_clientService.addPet(form))
                .withCallback(new Callback<Integer>() {
                    @Override
                    public void onResponse(Call<Integer> call, Response<Integer> response) {
                        pet.setId(response.body());
                        callback.accept(pet);
                    }

                    @Override
                    public void onFailure(Call<Integer> call, Throwable t) {
                        callback.accept(null);
                    }
                })
                .proceed();
    }

    @Override
    public void getControllerList(Consumer<List<NodeController>> callback) {
        new Retrier<List<NodeController>>()
                .withCall(_clientService.getControllers())
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getEnvironment(String id, Consumer<Environment> callback) {
        new Retrier<Environment>()
                .withCall(_clientService.getEnvironmentInfo(id))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getEnvironmentList(Consumer<List<Environment>> callback) {
        new Retrier<List<Environment>>()
                .withCall(_clientService.getAllEnvironments())
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void applyPetMigration(int petId, String envId, Consumer<Result> callback) {
        new Retrier<Void>()
                .withCall(_clientService.migratePet(petId, envId))
                .withCallback(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.code() != 200)
                            callback.accept(new Result.Error(new Exception("API error")));
                        else
                            callback.accept(new Result.Success<Void>());
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        callback.accept(new Result.Error(t));
                    }
                })
                .proceed();
    }

    /**
     * Converts a java.util.Function into a retrofit Callback.
     */
    private <TInput> Callback<TInput> _callbackFromConsumer(Consumer<TInput> f) {
        return new Callback<TInput>() {
            @Override
            public void onResponse(Call<TInput> call, Response<TInput> response) {
                if (200 <= response.code()  && response.code() < 300)
                    f.accept(response.body());
                else {
                    // TODO: limited retry via re-enqueuing call.
                    //       will require knowing how many times this request has been retried
                    Log.e("DAC", "Request " + call.request().url() + " failed: status code = " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TInput> call, Throwable t) {
                // convert to runtime exception (for now, in the future we should have failure
                // handlers too.

                // TODO: see if we can attempt a silent refresh and retry the call.
                throw new RuntimeException(t);
            }
        };
    }
}
