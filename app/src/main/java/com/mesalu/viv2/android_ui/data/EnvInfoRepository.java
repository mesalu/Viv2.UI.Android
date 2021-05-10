package com.mesalu.viv2.android_ui.data;

import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.http.IDataAccessClient;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;

import java.util.List;
import java.util.function.Consumer;

import retrofit2.Callback;

public class EnvInfoRepository {
    private static EnvInfoRepository instance;
    private IDataAccessClient accessClient;

    private EnvInfoRepository() {
        accessClient = ClientFactory.getDataClient();

    }

    public static EnvInfoRepository getInstance() {
        if (instance == null) instance = new EnvInfoRepository();
        return instance;
    }

    /**
     * Acquires the current list of node controllers for the signed in user and sends it to the
     * callback. If this information is cached & not aged out then it'll be supplied from there.
     * If it is aged out, then it is possible to see two callbacks made - first with cached info
     * and again with fresh info.
     */
    public void getControllerList(Consumer<List<NodeController>> callback) {
        accessClient.getControllerList(callback);
    }

    public void getEnvironment(String id, Consumer<Environment> callback) {
        accessClient.getEnvironment(id, callback);
    }

}
