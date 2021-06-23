package com.mesalu.viv2.android_ui.ui.events;

import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;

public class ChartTargetEvent extends ConsumableEvent<ChartTarget> {
    public enum ViewModifier {
        /**
         * Indicates that the chart should not be shown if hidden, and should not be hidden if
         * shown.
         */
        NO_MODIFIER,

        /**
         * Indicates that the chart should be shown, regardless of its current state.
         * (Subject to transitional animation)
         */
        SHOW,

        /**
         * Indicates that the chart should be hidden, regardless of its current state.
         * (Subject to transitional animation)
         */
        HIDE
    }

    private final ViewModifier viewModifier;

    public ChartTargetEvent(ChartTarget target, ViewModifier modifier) {
        super(target);
        viewModifier = modifier;
    }

    public ViewModifier getViewModifier() {
        return viewModifier;
    }
}
