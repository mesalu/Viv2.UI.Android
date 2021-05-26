package com.mesalu.viv2.android_ui.data.http.okhttp.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.http.ILoginClient;
import com.mesalu.viv2.android_ui.data.http.okhttp.OkHttpClientFactory;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import java.util.UUID;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class LoginClient implements ILoginClient {

    // TODO: move this to a more appropriate location.
    static final class ApiLoginResponse {
        static final class AccessToken {
            public String token;
            public int expiresIn;
        }
        static final class RefreshToken {
            public String token;
            public String issuedBy;
            public String issuedTo;
            public String expiresAt;
        }

        public String userName;
        public AccessToken accessToken;
        public RefreshToken refreshToken;
    }

    static final class LoginForm {
        String userName;
        String password;
    }

    static final class RefreshForm {
        String encodedToken;
        String userId;
    }

    interface ILoginService {
        @POST("auth/login")
        Call<ApiLoginResponse> login(@Body LoginForm form);

        @POST("token/refresh")
        Call<ApiLoginResponse> refresh(@Body RefreshForm form);
    }

    ILoginService _service;

    public LoginClient() {
        _service = OkHttpClientFactory.getInstance()
                .buildRetrofit().create(ILoginService.class);
    }

    @Override
    public void login(@NonNull String username, @NonNull String password, @NonNull Consumer<TokenSet> onSuccess, @NonNull Consumer<Throwable> onError) {
        LoginForm form = new LoginForm();
        form.userName = username;
        form.password = password;

        Log.d("LC", "Attempting to sign in!");

        // meh way to address the issue, but as long as we ensure that the builder's type parameters
        // match up to the retrier type parameters we should be okay.
        Retrier<ApiLoginResponse> retrier = new Retrier<ApiLoginResponse>()
                .withCall(_service.login(form))
                .withTryCount(5)
                .withPredicate((call, response) -> (200 <= response.code() && response.code() <= 300) || response.code() == 400)
                .withCallback(new Callback<ApiLoginResponse>() {
                    @Override
                    public void onResponse(Call<ApiLoginResponse> call, Response<ApiLoginResponse> response) {
                        Log.d("LC", "Login processing response!");
                        if (response.code() == 400) {
                            onError.accept(new Exception("Bad authentication"));
                            return;
                        }

                        TokenSet tokens = tokensFromResponse(response.body());
                        onSuccess.accept(tokens);
                    }

                    @Override
                    public void onFailure(Call<ApiLoginResponse> call, Throwable t) {
                        Log.e("LC", "Login failed :(");
                        Log.e("LC", t.toString());
                        onError.accept(t);
                    }
                });

        retrier.proceed();
    }

    @Override
    public void refresh(@NonNull Consumer<TokenSet> onSuccess,
                        @NonNull Consumer<Throwable> onFailure) {
        TokenSet tokens = LoginRepository.getInstance().getTokens();

        // compose RefreshForm from TokenSet
        RefreshForm form = new RefreshForm();
        form.userId = tokens.getUserId().toString();
        form.encodedToken = tokens.getRefreshToken();

        Retrier<ApiLoginResponse> retrier = new Retrier<ApiLoginResponse>()
                .withTryCount(5)
                .withCall(_service.refresh(form))
                .withPredicate((call, response) -> (200 <= response.code() && response.code() <= 300))
                .withCallback(new Callback<ApiLoginResponse>() {
                    @Override
                    public void onResponse(Call<ApiLoginResponse> call, Response<ApiLoginResponse> response) {
                        Log.d("LoginClient", "onResponse for refresh invoked correctly");
                        onSuccess.accept(tokensFromResponse(response.body()));
                    }

                    @Override
                    public void onFailure(Call<ApiLoginResponse> call, Throwable t) {
                        onFailure.accept(t);
                    }
                });

        retrier.proceed();
    }

    private TokenSet tokensFromResponse(ApiLoginResponse response) {
        TemporalAccessor accessor = DateTimeFormatter
                .ISO_DATE_TIME
                .parse(response.refreshToken.expiresAt);

        ZonedDateTime refreshExpiry = ZonedDateTime.from(accessor);

        // now figure out when the access token expires:
        ZonedDateTime accessExpiry = ZonedDateTime.now().plusSeconds(response.accessToken.expiresIn);

        return new TokenSet(response.userName,
                response.accessToken.token,
                response.refreshToken.token,
                accessExpiry,
                refreshExpiry,
                UUID.fromString(response.refreshToken.issuedTo));
    }
}
