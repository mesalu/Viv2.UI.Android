package com.mesalu.viv2.android_ui.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.mesalu.viv2.android_ui.data.model.LoggedInUser;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class LoginRepository {

    private static volatile LoginRepository instance;
    private LoginDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore

    // NOTE: stored in memory only, so we shouldn't need to worry overmuch about
    // encrypting.
    private MutableLiveData<LoggedInUser> user = null;

    // private constructor : singleton access
    // TODO: use dependency injection (via Dagger?) to get login data source impl.
    private LoginRepository(LoginDataSource dataSource) {
        this.dataSource = dataSource;
        dataSource.setListener(new LoginDataSource.LoginEventListener() {
            @Override
            public void onSuccess(Result<LoggedInUser> result) {
                if (result instanceof Result.Success)
                    setLoggedInUser(((Result.Success<LoggedInUser>) result).getData());
            }

            @Override
            public void onFailure(Result.Error error) {
                Log.d("LRepo", "made it to onFailure");
                setLoggedInUser(null);
            }
        });

        this.user = new MutableLiveData<>();
    }

    public static LoginRepository getInstance(LoginDataSource dataSource) {
        if (instance == null) {
            instance = new LoginRepository(dataSource);
        }
        return instance;
    }

    public static LoginRepository getInstance() {
        if (instance == null) {
            throw new RuntimeException("Data source required for first-initialization");
        }
        return instance;
    }

    public static boolean loginRequired() {
        if (instance == null) return true;
        return !(instance.isLoggedIn());
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public void logout() {
        user = null;
        dataSource.logout();
    }

    private void setLoggedInUser(LoggedInUser user) {
        this.user.setValue(user);
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    public void assignObserver(LifecycleOwner owner, Observer<LoggedInUser> observer) {
        this.user.observe(owner, observer);
    }

    public void assignObserver(Observer<LoggedInUser> observer) {
        this.user.observeForever(observer);
    }

    public void removeObserver(Observer<LoggedInUser> observer) {
        this.user.removeObserver(observer);
    }


    // Login is handled asynchronously via callbacks into the mainthread,
    // as such, this method does not return any meaningful data to the caller.
    // Instead, entities interested in login results should observe for changes
    // on login result.
    public void login(Context context, String username, String password) {
        // handle login
        dataSource.login(context, username, password);
    }
}