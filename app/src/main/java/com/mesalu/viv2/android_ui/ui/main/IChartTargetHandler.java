package com.mesalu.viv2.android_ui.ui.main;

import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;
import com.mesalu.viv2.android_ui.ui.events.ChartTargetEvent;

interface IChartTargetHandler {
    /**
     * Signals the main activity, or consuming activity, that a chart should be configured
     * to show data as specified by `target.
     *
     * @param target the entity for which sample data should be loaded and displayed.
     * @param desiredViewState Indicates the view state the caller wishes the chart fragment to be in
     *                         upon fulfillment of the target request.
     */
    void signalChartTarget(ChartTarget target, ChartTargetEvent.ViewModifier desiredViewState);

    /**
     * As signalChartTarget(ChartTarget, ViewModifier) with desiredViewState set to SHOW.
     *
     * Signals the main activity, or consuming activity, that a chart should be configured
     * to show data as specified by `target`.
     *
     * @param target the entity for which sample data should be loaded and displayed.
     */
    void signalChartTarget(ChartTarget target);
}
