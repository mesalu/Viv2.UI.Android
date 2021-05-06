package com.mesalu.viv2.android_ui.data.http.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.http.IDataAccessClient;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
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
                .withCallback(_callbackFromFunction(callback))
                .proceed();
    }

    @Override
    public void getPet(int id, Consumer<Pet> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        new Retrier<Pet>()
                .withCall(_clientService.getPetById(_headersFromTokens(tokens), id))
                .withCallback(_callbackFromFunction(callback))
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
                .withCallback(_callbackFromFunction(callback))
                .proceed();
    }

    @Override
    public void getPreliminaryPetInfo(int id, Consumer<PreliminaryPetInfo> callback) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();
        Call<PreliminaryPetInfo> call = _clientService
                .getPreliminaryInfo(_headersFromTokens(tokens), id);

        new Retrier<PreliminaryPetInfo>()
                .withCall(call)
                .withCallback(_callbackFromFunction(callback))
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
    private <TInput> Callback<TInput> _callbackFromFunction(Consumer<TInput> f) {
        return new Callback<TInput>() {
            @Override
            public void onResponse(Call<TInput> call, Response<TInput> response) {
                if (200 <= response.code()  && response.code() < 300)
                    f.accept(response.body());
                else {
                    // TODO: limited retry via re-enqueuing call.
                    //       will require knowing how many times this request has been retried
                    Log.e("DAC", "Request " + call.request().url() + "failed: status code = " + response.code());
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
