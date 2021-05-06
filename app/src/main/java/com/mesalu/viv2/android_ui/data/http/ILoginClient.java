package com.mesalu.viv2.android_ui.data.http;

import androidx.annotation.NonNull;

import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ILoginClient {
    /**
     * Attempts to login against the API with the given credentials.
     * If successful, onSuccess is invoked with a TokenSet extracted from
     * the API's response. If authentication fails for any reason, onError is called.
     *
     * @param username self explanatory
     * @param password self explanatory
     * @param onSuccess invoked when authentication succeeds
     * @param onError invoked when authentication fails for any reason
     */
    void login(@NonNull String username, @NonNull String password,
               @NonNull Consumer<TokenSet> onSuccess,
               @NonNull Consumer<Throwable> onError);

    /**
     * Requests a refresh of the token set, using the information cached in TokenStore to instigate
     * the exchange.
     * @param onSuccess
     * @param onFailure
     */
    void refresh(@NonNull Consumer<TokenSet> onSuccess,
                 @NonNull Consumer<Throwable> onFailure);
}
