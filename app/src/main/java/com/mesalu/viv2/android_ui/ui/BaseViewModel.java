package com.mesalu.viv2.android_ui.ui;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

/**
 * ViewModels in this project are frequently reimplementing one-off observers. (ex. populating
 * views within a recycler view - once loaded we don't want a lingering observer overwriting
 * a rebound view holder's content.)
 * So this base class provides a method to reduce boilerplate in one-off observers.
 */
public abstract class BaseViewModel extends ViewModel {
    /**
     * Clears any data that is contained within the view model instance that is tied to a particular
     * user or may be considered sensitive.
     */
    public abstract void clearUserSensitiveData();

    /**
     * Creates a wrapper around observer that ensures that the observer is only invoked once
     * and then removed from the observable
     * @param observable an observable live data instance.
     * @param owner the lifecycle owner to use when observing on observable.
     * @param observer the observer to be invoked once
     * @param <T> the target data type
     */
    public <T> void observeOnce(LiveData<T> observable, LifecycleOwner owner, Observer<T> observer) {
        observable.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(T t) {
                observable.removeObserver(this);
                observer.onChanged(t);
            }
        });
    }
}
