package com.mesalu.viv2.android_ui.data.http;


import android.content.Context;

/**
 * Lightweight factory for instancing concrete versions of client interfaces.
 * May eventually be replaced by DI.
 */
public final class ClientFactory {

    public static IDataAccessClient getDataClient() {
        return new com.mesalu.viv2.android_ui.data.http.retrofit.DataAccessClient();
    }

    public static ILoginClient getLoginClient() {
        return new com.mesalu.viv2.android_ui.data.http.retrofit.LoginClient();
    }
}
