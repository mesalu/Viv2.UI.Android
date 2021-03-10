package com.mesalu.viv2.android_ui.data;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.mesalu.viv2.android_ui.data.model.LoggedInUser;
import com.mesalu.viv2.android_ui.ui.login.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {
    public interface LoginEventListener {
        void onSuccess(Result<LoggedInUser> user);
        void onFailure(Result.Error error);
    }

    protected LoginEventListener listener;

    /**
     * Handles logging in via an https connection
     *
     * Success/failure is communicated back out via the configured LoginEventListener.
     *
     * @param context android context, required for getting an HttpClient.
     * @param username self explanatory.
     * @param password self explanatory.
     */
    public void login(Context context, String username, String password) {
        try {
            String url = "https://192.168.0.10:5001/api/login";

            JSONObject loginObject = new JSONObject();
            try {
                loginObject.put("username", username);
                loginObject.put("password", password);

                Log.d("LDS", "Set up login object");
            }
            catch (JSONException e) {
                // no big deal?
                Log.d("LDS", "Failed to set up login object");
            }

            HttpClient client = HttpClient.getInstance(context);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    url,
                    loginObject,
                    response -> {
                        Log.d("LDS", "Login result: " + response.toString());
                        try {
                            UUID userId = UUID.fromString(response.getString("userId"));
                            String displayName = response.getString("displayName");
                            String token = response.getString("token");

                            LoggedInUser user = new LoggedInUser(
                                    userId,
                                    displayName,
                                    token
                            );

                            if (listener != null)
                                listener.onSuccess(new Result.Success<>(user));
                        }
                        catch (JSONException exc) {
                            Log.e("LDS", "Malformed login result object");
                            if (listener != null) listener.onFailure(new Result.Error(exc));
                        }
                    },
                    error -> {
                        // TODO: Handle error
                        Log.d("LDS", "Error when making request! " + error.toString());
                        if (listener != null) {
                            listener.onFailure(new Result.Error(error));
                        }
                    });

            client.add(request);
            Log.d("LDS", "added request to queue!");
        } catch (Exception e) {
            if (listener != null)
                listener.onFailure(new Result.Error(e));
        }
    }

    public void setListener(LoginEventListener listener) {
        this.listener = listener;
    }

    public void logout() {
        // TODO: revoke authentication
    }
}