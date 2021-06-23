package com.mesalu.viv2.android_ui.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mesalu.viv2.android_ui.ui.BaseViewModel;
import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;
import com.mesalu.viv2.android_ui.ui.events.ChartTargetEvent;

/**
 * A view model specifically for the main activity, nav fragments may use this to communicate
 * particular actions - such as showing a chart fragment - but otherwise its used to store a
 * view state for the MainActivity.
 */
public class MainActivityViewModel extends BaseViewModel implements IChartTargetHandler {
    private final MutableLiveData<ChartTargetEvent> chartTargetEvent;

    public MainActivityViewModel() {
        chartTargetEvent = new MutableLiveData<>();
    }

    protected LiveData<ChartTargetEvent> getChartTargetEvent() {
        return chartTargetEvent;
    }

    /**
     * Signals the main activity, or consuming activity, that a chart should be configured
     * to show data as specified by `target.
     *
     * @param target the entity for which sample data should be loaded and displayed.
     * @param desiredViewState Indicates the view state the caller wishes the chart fragment to be in
     *                         upon fulfillment of the target request.
     */
    @Override
    public void signalChartTarget(ChartTarget target, ChartTargetEvent.ViewModifier desiredViewState) {
        if (target == null && desiredViewState != ChartTargetEvent.ViewModifier.HIDE)
            throw new IllegalArgumentException("target must be non-null for plausibly-shown view states");

        chartTargetEvent.setValue(new ChartTargetEvent(target, desiredViewState));
    }

    /**
     * As signalChartTarget(ChartTarget, ViewModifier) with desiredViewState set to SHOW.
     *
     * Signals the main activity, or consuming activity, that a chart should be configured
     * to show data as specified by `target`.
     *
     * @param target the entity for which sample data should be loaded and displayed.
     */
    public void signalChartTarget(ChartTarget target) {
        signalChartTarget(target, (target == null) ? ChartTargetEvent.ViewModifier.HIDE : ChartTargetEvent.ViewModifier.SHOW);
    }
}
