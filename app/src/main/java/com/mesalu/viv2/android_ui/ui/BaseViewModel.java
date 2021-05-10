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
public class BaseViewModel extends ViewModel {

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
