package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mesalu.viv2.android_ui.ui.BaseViewModel;
import com.mesalu.viv2.android_ui.ui.events.SimpleEvent;

/**
 * Base class for view models that support communicating FAB button presses.
 *
 * Fragments that will be used in Overview activity will likely need to patch into FAB
 * button presses. There are a few decent ways to communicate that button press to the fragment
 * for handling, but since we have these fragments fairly coupled to corresponding ViewModels, it
 * makes sense to have a base ViewModel that provides the groundwork for the real ViewModel
 * implementations.
 */
public class FabAwareViewModel extends BaseViewModel {
    protected MutableLiveData<SimpleEvent> fabEvent;

    protected FabAwareViewModel() {
        fabEvent = new MutableLiveData<>();
    }

    /**
     * Notify fab-listeners on this view model of the FABs button press.
     */
    public void signal() {
        fabEvent.setValue(new SimpleEvent());
    }

    public LiveData<SimpleEvent> getFabSignal() {
        return fabEvent;
    }
}
