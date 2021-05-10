package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.mesalu.viv2.android_ui.data.EnvInfoRepository;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentInfoViewModel extends FabAwareViewModel {
    private EnvInfoRepository envInfoRepository;
    private MutableLiveData<List<NodeController>> controllers;
    private Map<String, MutableLiveData<Environment>> environments;

    public EnvironmentInfoViewModel() {
        super();
        envInfoRepository = EnvInfoRepository.getInstance();
        controllers = new MutableLiveData<>();
        environments = new HashMap<>();
    }

    public LiveData<List<NodeController>> getControllersObservable() {
        if (controllers.getValue() == null) updateControllers();
        return controllers;
    }

    public void updateControllers() {
        envInfoRepository.getControllerList(l -> controllers.setValue(l));
    }

    public LiveData<Environment> getEnvironmentObservable(String id) {
        if (!environments.containsKey(id)) {
            final MutableLiveData<Environment> env = new MutableLiveData<>();
            environments.put(id, env);
            envInfoRepository.getEnvironment(id, env::setValue);
        }
        return environments.get(id);
    }

    /**
     * Convenience method, wraps the given observer in a format that
     * ensures that the observer is invoked only once and then removed.
     */
    public void observeEnvironmentOnce(String id, LifecycleOwner owner, final Observer<Environment> observer) {
        final LiveData<Environment> observable = getEnvironmentObservable(id);
        observeOnce(observable, owner, observer);
    }
}