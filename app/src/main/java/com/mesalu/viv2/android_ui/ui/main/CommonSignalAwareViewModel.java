package com.mesalu.viv2.android_ui.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mesalu.viv2.android_ui.ui.BaseViewModel;
import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;
import com.mesalu.viv2.android_ui.ui.events.ChartTargetEvent;
import com.mesalu.viv2.android_ui.ui.events.ConsumableEvent;
import com.mesalu.viv2.android_ui.ui.events.SimpleEvent;

import java.util.UUID;

/**
 * Base class for view models that support communicating FAB button presses & other common
 * signals that the host activity may want to convey.
 *
 * Fragments that will be used in Overview activity will likely need to patch into FAB
 * button presses. There are a few decent ways to communicate that button press to the fragment
 * for handling, but since we have these fragments fairly coupled to corresponding ViewModels, it
 * makes sense to have a base ViewModel that provides the groundwork for the real ViewModel
 * implementations.
 */
public abstract class CommonSignalAwareViewModel extends BaseViewModel {
    private final MutableLiveData<SimpleEvent> fabEvent;
    private final MutableLiveData<SimpleEvent> refreshEvent;
    private final MutableLiveData<SimpleEvent> uiUpdateEvent;

    protected CommonSignalAwareViewModel() {
        fabEvent = new MutableLiveData<>();
        refreshEvent = new MutableLiveData<>();
        uiUpdateEvent = new MutableLiveData<>();
    }

    /**
     * Notify fab-listeners on this view model of the FABs button press.
     */
    public void signalFab() {
        fabEvent.setValue(new SimpleEvent());
    }

    public LiveData<SimpleEvent> getFabSignal() {
        return fabEvent;
    }

    public void signalRefresh() {
        refreshEvent.setValue(new SimpleEvent());
    }

    public LiveData<SimpleEvent> getRefreshSignal() {
        return refreshEvent;
    }

    public void signalUiUpdate() {
        uiUpdateEvent.setValue(new SimpleEvent());
    }

    public void signalUiUpdateFromBackground() {
        uiUpdateEvent.postValue(new SimpleEvent());
    }

    public LiveData<SimpleEvent> getUiUpdateSignal() {
        return uiUpdateEvent;
    }
}
