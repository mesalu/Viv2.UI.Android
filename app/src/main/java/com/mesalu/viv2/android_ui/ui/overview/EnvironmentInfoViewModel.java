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

    // The different ways in which Environment data will be loaded, care will need to be taken
    // to ensure that their state's remain synchronized.
    private final Map<String, MutableLiveData<Environment>> environmentsMap;
    private final MutableLiveData<List<Environment>> envListLiveData;

    public EnvironmentInfoViewModel() {
        super();
        envInfoRepository = EnvInfoRepository.getInstance();
        controllers = new MutableLiveData<>();
        environmentsMap = new HashMap<>();
        envListLiveData = new MutableLiveData<>();
    }

    public LiveData<List<NodeController>> getControllersObservable() {
        if (controllers.getValue() == null) updateControllers();
        return controllers;
    }

    public void updateControllers() {
        envInfoRepository.getControllerList(l -> controllers.setValue(l));
    }

    public LiveData<Environment> getEnvironmentObservable(String id) {
        if (!environmentsMap.containsKey(id)) {
            MutableLiveData<Environment> env = new MutableLiveData<>();
            environmentsMap.put(id, env);
            envInfoRepository.getEnvironment(id, this::lockstepEnvUpdate);
        }
        return environmentsMap.get(id);
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
     * Note: the list given to observers is unmodifiable.
     */
    public LiveData<List<Environment>> getEnvListObservable() {
        // kick off a refresh, just in case.
        envInfoRepository.getEnvironmentList(l -> l.forEach(this::lockstepEnvUpdate));

        return envListLiveData;
    }

    private void lockstepEnvUpdate(Environment environment) {
        if (!environmentsMap.containsKey(environment.getId()))
            environmentsMap.put(environment.getId(), new MutableLiveData<>());
        environmentsMap.get(environment.getId()).setValue(environment);

        // O(n) sweep of hash map values... unideal.
        // compose a list of Environment values from those in the hash set.
        ArrayList<Environment> intermediate = new ArrayList<>();
        for (LiveData<Environment> ld : environmentsMap.values()) {
            Environment e = ld.getValue();
            if (ld.getValue() != null)
                intermediate.add(e);
        }

        envListLiveData.setValue(Collections.unmodifiableList(intermediate));
    }
}