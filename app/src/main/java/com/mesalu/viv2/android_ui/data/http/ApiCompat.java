package com.mesalu.viv2.android_ui.data.http;

import android.net.Uri;
import android.util.Log;

import com.mesalu.viv2.android_ui.BuildConfig;

public final class ApiCompat {
    public static final String apiUrl = (BuildConfig.DEBUG) ? "https://192.168.0.10:5001/api/"
           : "https://viv2api.azurewebsites.net/api/";

    /**
     * Generates a URI appropriate for accessing a pet's image given its id.
     * the API doesn't (yet) include this in the pet model.
     */
    public static Uri imageUriFromPet(int petId) {
        Log.d("ApiCompat", "petId = " + petId);
        return Uri.parse(apiUrl + "pet/" + petId + "/image");
    }
}
