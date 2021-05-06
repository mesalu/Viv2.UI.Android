package com.mesalu.viv2.android_ui.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.http.ILoginClient;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepository {

    private static volatile LoginRepository instance;
    private ILoginClient client;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore

    // NOTE: stored in memory only, so we shouldn't need to worry overmuch about
    // encrypting.
    private MutableLiveData<TokenSet> tokens = null;

    // private constructor : singleton access
    // TODO: use dependency injection (via Dagger?) to get login data source impl.
    private LoginRepository() {
        tokens = new MutableLiveData<>();
        client = ClientFactory.getLoginClient();
    }

    public static LoginRepository getInstance() {
        if (instance == null) {
            // TODO: fetch from DI / service map
            instance = new LoginRepository();
        }
        return instance;
    }

    /**
     * returns true if there exists a token set, but its access token has expired.
     * returns false if tokens are loaded & not expired.
     *
     * IF tokens aren't loaded, an exception is thrown.
     */
    public boolean needsRefresh() {
        if (tokens == null || tokens.getValue() == null)
            throw new IllegalStateException("No tokens loaded");

        return tokens.getValue().getAccessExpiry().compareTo(ZonedDateTime.now()) <= 0;
    }

    /**
     * Indicates that the active token set is completely valid & ready to go.
     */
    public boolean isLoggedIn() {
        return tokens != null
                && tokens.getValue() != null
                && tokens.getValue().getAccessExpiry().compareTo(ZonedDateTime.now()) > 0;
    }

    public void logout() {
        tokens.setValue(null);
    }

    public TokenSet getTokens() {
        if (tokens == null) return null;
        return tokens.getValue();
    }

    public LiveData<TokenSet> getObservable() {
        return tokens;
    }

    // Login is handled asynchronously via callbacks into the mainthread,
    // as such, this method does not return any meaningful data to the caller.
    // Instead, entities interested in login results should observe for changes
    // on login result.
    public void login(String username, String password) {
        // handle login
        client.login(username, password,
                tokenSet -> tokens.setValue(tokenSet),
                throwable -> {
                    Log.d("LoginRepo", "Failed to login: " + throwable.toString());
                    tokens.setValue(null);
                });
    }

    /**
     * Kicks off a request to refresh tokens, if successful an update on
     * all assigned observers will be called.
     *
     * In case of failure, onFailure.accept will be called.
     */
    public void attemptRefresh(@NonNull Consumer<Throwable> onFailure) {
        client.refresh(tokenSet -> {
            Log.d("LoginRepo", "Accepting token set...");
            tokens.setValue(tokenSet);
        },
                throwable -> onFailure.accept(throwable));
    }
}