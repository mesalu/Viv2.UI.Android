package com.mesalu.viv2.android_ui.data.http.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.http.IDataAccessClient;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NewPetForm;
import com.mesalu.viv2.android_ui.data.model.NodeController;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.data.model.Species;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataAccessClient implements IDataAccessClient {
    private final IApiService _clientService;

    public DataAccessClient() {
        _clientService = new HttpClientFactory()
                .composeClient()
                .create(IApiService.class);
    }

    @Override
    public void getPetIdList(@NonNull Consumer<List<Integer>> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();
        new Retrier<List<Integer>>()
                .withCall(_clientService.getPetIdList(_headersFromTokens(tokens)))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getPet(int id, Consumer<Pet> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        new Retrier<Pet>()
                .withCall(_clientService.getPetById(_headersFromTokens(tokens), id))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getSamplesInDateRange(Date a, Date b, Consumer<List<EnvDataSample>> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();
        String start = DateTimeFormatter.ISO_INSTANT.format(a.toInstant());
        String end = DateTimeFormatter.ISO_INSTANT.format(b.toInstant());

        Call<List<EnvDataSample>> call = _clientService
                .getSamplesInRange(_headersFromTokens(tokens), start, end);

        new Retrier<List<EnvDataSample>>()
                .withCall(call)
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getPreliminaryPetInfo(int id, Consumer<PreliminaryPetInfo> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();
        Call<PreliminaryPetInfo> call = _clientService
                .getPreliminaryInfo(_headersFromTokens(tokens), id);

        new Retrier<PreliminaryPetInfo>()
                .withCall(call)
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getSpeciesList(Consumer<List<Species>> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        new Retrier<List<Species>>()
                .withCall(_clientService.getSpeciesInfo(_headersFromTokens(tokens)))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void addPet(final Pet pet, Consumer<Pet> callback) {
        // convert to "NewPetForm" dto - API side.
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        // convert pet to NewPetForm:
        NewPetForm form = new NewPetForm();
        form.setName(pet.getName());
        form.setMorph(pet.getMorph());
        form.setSpeciesId(pet.getSpecies().getId());

        new Retrier<Integer>()
                .withCall(_clientService.addPet(_headersFromTokens(tokens), form))
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
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        new Retrier<List<NodeController>>()
                .withCall(_clientService.getControllers(_headersFromTokens(tokens)))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getEnvironment(String id, Consumer<Environment> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        new Retrier<Environment>()
                .withCall(_clientService.getEnvironmentInfo(_headersFromTokens(tokens), id))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    @Override
    public void getEnvironmentList(Consumer<List<Environment>> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        new Retrier<List<Environment>>()
                .withCall(_clientService.getAllEnvironments(_headersFromTokens(tokens)))
                .withCallback(_callbackFromConsumer(callback))
                .proceed();
    }

    private Map<String, String> _headersFromTokens(TokenSet tokens) {
        HashMap<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + tokens.getAccessToken());
        return map;
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
