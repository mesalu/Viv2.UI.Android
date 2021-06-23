package com.mesalu.viv2.android_ui.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import android.util.Patterns;

import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.model.TokenSet;

public class LoginViewModel extends ViewModel {

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    private Observer<TokenSet> tokensObserver;

    LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
        this.tokensObserver = tokens -> {
            if (tokens != null)
                loginResult.setValue(new LoginResult(new LoggedInUserView(tokens.getDisplayName())));
            else {
                // changing to null could be indicative of errors,
                // however it would also catch when the data is first assigned.
                loginResult.setValue(new LoginResult(R.string.login_failed));
            }
        };
        loginRepository.getObservable().observeForever(tokensObserver);
    }

    @Override
    protected void onCleared() {
        loginRepository.getObservable().removeObserver(tokensObserver);
        super.onCleared();
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        loginRepository.login(username, password);
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}