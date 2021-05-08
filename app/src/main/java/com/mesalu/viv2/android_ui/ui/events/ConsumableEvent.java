package com.mesalu.viv2.android_ui.ui.events;

/**
 * Used in conjunction with live data for communicating events.
 * E.g., the press of a FAB could emit an event to a corresponding view model to trigger the
 * specialized handling of that button press.
 *
 * Observers on that live data notification list can then specify if they've consumed the data
 * thus preventing other observers from acting on it.
 *
 * Pitfalls:
 *  - notification processing proceeds whether or not the event has already been consumed.
 *  - all observers must be cognisant of how they use the Event.
 *
 */
public class ConsumableEvent <T> {
    private T data;
    protected boolean consumed;

    ConsumableEvent(T data) {
        this.data = data;
        consumed = false;
    }

    /**
     * Used by observer's whose intent is to 'consume' the event,
     * @return
     */
    public synchronized T consume() {
        if (consumed) return null;
        consumed = true;
        return data;
    }

    public T peek() {
        return data;
    }
}
