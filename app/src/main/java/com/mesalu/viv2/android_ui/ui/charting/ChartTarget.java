package com.mesalu.viv2.android_ui.ui.charting;

import java.util.UUID;

/**
 * Represents an object for which EnvDataSamples are associated to (a pet, an environment, etc.)
 */
public class ChartTarget {
    public enum TargetType {
        Pet, Environment
    }
    Object id;
    TargetType targetType;

    public ChartTarget(Object id, TargetType targetType) {
        this.id = id;
        this.targetType = targetType;

        validateIdType();
    }

    private void validateIdType() {
        if (targetType == TargetType.Pet && !(id instanceof Integer))
            throw new IllegalStateException();

        else if (targetType == TargetType.Environment && !(id instanceof UUID))
            throw new IllegalStateException();
    }
}
