package com.mesalu.viv2.android_ui;

import android.content.Context;

/**
 * An extension of android.app.Application that permits accessing
 * application context globally.
 *
 * Should only be used in code where context is implied but abstraction
 * requires that context not be provided.
 */
public class Application extends android.app.Application {
    // NOTE: generally I'm not happy with this and wanted to avoid it
    //       the nail in the coffin was when I was trying to set up a silent refresh
    //       executor and didn't want to plumb a context from that into HttpClientFactory
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        Application.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return Application.context;
    }
}
