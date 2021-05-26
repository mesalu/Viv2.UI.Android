package com.mesalu.viv2.android_ui.data.http;


import com.mesalu.viv2.android_ui.Application;
import com.mesalu.viv2.android_ui.data.http.okhttp.picasso.ImageClient;

/**
 * Lightweight factory for instancing concrete versions of client interfaces.
 * May eventually be replaced by DI.
 */
public final class ClientFactory {

    public static IDataAccessClient getDataClient() {
        return new com.mesalu.viv2.android_ui.data.http.okhttp.retrofit.DataAccessClient();
    }

    public static ILoginClient getLoginClient() {
        return new com.mesalu.viv2.android_ui.data.http.okhttp.retrofit.LoginClient();
    }

    public static IImageClient getImageClient() {
        return new com.mesalu.viv2.android_ui.data.http.okhttp.picasso.ImageClient(Application.getAppContext());
    }
}
