package com.mesalu.viv2.android_ui.data;

import android.util.Log;

import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.http.ILoginClient;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.util.function.Consumer;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 *
 * TODO: convert to interface or deprecate & merge into data-acquisition stuff.
 */
@Deprecated
public class LoginDataSource {
    public interface LoginEventListener {
        void onSuccess(Result<TokenSet> userTokens);
        void onLoginFailure(Result.Error error);
    }

    protected LoginEventListener listener;

    /**
     * Handles logging in via an https connection
     *
     * Success/failure is communicated back out via the configured LoginEventListener.
     *
     * @param username self explanatory.
     * @param password self explanatory.
     */
    public void login(String username, String password) {
        try {
            ILoginClient loginClient = ClientFactory.getLoginClient();
            loginClient.login(username, password,
                    tokenSet -> {
                        if (listener != null)
                            listener.onSuccess(new Result.Success<>(tokenSet));
                    },
                    throwable -> {
                        if (listener != null) listener.onLoginFailure(new Result.Error(throwable));
                    });
        } catch (Exception e) {
            Log.e("LDS", "blegh");
            Log.e("LDS", e.toString());
            if (listener != null)
                listener.onLoginFailure(new Result.Error(e));
        }
    }

    public void setListener(LoginEventListener listener) {
        this.listener = listener;
    }

    public void logout() {
        // TODO: revoke authentication
    }

    public void refresh() {
        refresh(null);
    }

    public void refresh(Consumer<Throwable> onError) {
        ILoginClient client = ClientFactory.getLoginClient();
        client.refresh(tokenSet -> {
                    // new token set, woo!
                    if (listener != null)
                        listener.onSuccess(new Result.Success<>(tokenSet));
                },
                throwable -> {
                    if (onError != null)
                        onError.accept(throwable);
                });
    }
}