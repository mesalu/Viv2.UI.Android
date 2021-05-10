package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mesalu.viv2.android_ui.data.EnvInfoRepository;
import com.mesalu.viv2.android_ui.data.model.NodeController;

import java.util.List;

public class EnvironmentInfoViewModel extends FabAwareViewModel {
    private EnvInfoRepository envInfoRepository;
    private MutableLiveData<List<NodeController>> controllers;

    public EnvironmentInfoViewModel() {
        super();
        envInfoRepository = EnvInfoRepository.getInstance();
        controllers = new MutableLiveData<>();
    }

    public LiveData<List<NodeController>> getControllersObservable() {
        if (controllers.getValue() == null) updateControllers();
        return controllers;
    }

    public void updateControllers() {
        envInfoRepository.getControllerList(l -> controllers.setValue(l));
    }
}