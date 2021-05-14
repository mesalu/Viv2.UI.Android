package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.mesalu.viv2.android_ui.data.EnvInfoRepository;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnvironmentInfoViewModel extends FabAwareViewModel {
    private final EnvInfoRepository envInfoRepository;
    private final MutableLiveData<List<NodeController>> controllers;
    private final HybridCollectionLiveData<String, Environment> environmentLiveData;

    public EnvironmentInfoViewModel() {
        super();
        envInfoRepository = EnvInfoRepository.getInstance();
        environmentLiveData = new HybridCollectionLiveData<>(Environment::getId);
        controllers = new MutableLiveData<>();
    }

    public LiveData<List<NodeController>> getControllersObservable() {
        if (controllers.getValue() == null) updateControllers();
        return controllers;
    }

    public void updateControllers() {
        envInfoRepository.getControllerList(controllers::setValue);
    }

    /**
     * Gets an observable that will receive updates for environments of the matching ID
     * as they arrive. Automatically requests an update.
     * @param id
     * @return
     */
    public LiveData<Environment> getEnvironmentObservable(String id) {
        return getEnvironmentObservable(id, true);
    }

    /**
     * Gets an observable that will receive updates for environments of the matching ID
     * as they arrive.
     * @param id
     * @param requestUpdate calls updateEnvironment(id) if true.
     * @return
     */
    public LiveData<Environment> getEnvironmentObservable(String id, boolean requestUpdate) {
        if (requestUpdate) updateEnvironment(id);
        return environmentLiveData.getObservableForId(id);
    }

    /**
     * Requests an update for the specified Environment.
     * Whether or not this update is fulfilled is dependent on the Env. repository.
     * @param id
     */
    public void updateEnvironment(String id) {
        envInfoRepository.getEnvironment(id, environmentLiveData::update);
    }

    /**
     * Convenience method, wraps the given observer in a format that
     * ensures that the observer is invoked only once and then removed.
     */
    public void observeEnvironmentOnce(String id, LifecycleOwner owner, Observer<Environment> observer) {
        final LiveData<Environment> observable = getEnvironmentObservable(id);
        observeOnce(observable, owner, observer);
    }

    /**
     * Gets an observable that receives batch updates of all environments.
     * Note: the list given to observers is unmodifiable
     *
     * @param requestUpdate if true, calls requestBatchEnvUpdate
     */
    public LiveData<List<Environment>> getEnvListObservable(boolean requestUpdate) {
        // kick off a refresh, just in case.
        if (requestUpdate) requestBatchEnvUpdate();
        return environmentLiveData.getListObservable();
    }

    /**
     * As getEnvListObservable(boolean), but defaults `requestUpdate` to true.
     * @return an observable live data that receives an updated list of environments.
     */
    public LiveData<List<Environment>> getEnvListObservable() {
        return getEnvListObservable(true);
    }

    /**
     * Requests an update of all environments from the repository layer.
     * The fullfillment of this request will be dependent on the repository layer, but if
     * successful then all observers of any Environment related data should receive an update.
     */
    public void  requestBatchEnvUpdate() {
        envInfoRepository.getEnvironmentList(environmentLiveData::batchUpdate);
    }
}