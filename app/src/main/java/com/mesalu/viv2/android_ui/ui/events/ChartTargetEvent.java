package com.mesalu.viv2.android_ui.ui.events;

import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;

public class ChartTargetEvent extends ConsumableEvent<ChartTarget> {
    private final boolean shouldShowIfHidden;

    public ChartTargetEvent(ChartTarget target, boolean showIfHidden) {
        super(target);
        shouldShowIfHidden = showIfHidden;
    }

    public boolean shouldShowChart() {
        return shouldShowIfHidden;
    }
}
