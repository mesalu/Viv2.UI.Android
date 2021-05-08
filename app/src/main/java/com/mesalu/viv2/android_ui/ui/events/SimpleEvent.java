package com.mesalu.viv2.android_ui.ui.events;

/**
 * Specialization of ConsumeableEvent for 'dataless' events.
 */
public class SimpleEvent extends ConsumableEvent<Boolean> {

    /**
     * Preferred constructor for SimpleEvent.
     */
    public SimpleEvent() {
        super(false);
    }

    @Override
    public synchronized Boolean consume() {
        if (consumed) return false;
        consumed = true;
        return true;
    }
}
