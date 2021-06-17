package com.mesalu.viv2.android_ui.ui.main.data_entry;

/*
 * General dialogfragment blurb here:
 *
 * There's an... interesting... incompatibility in android's tools w.r.t. dialog fragments & live
 * data. Ideally, fragments use their root view's lifecycle owner for observing live data (as
 * the lifecycle of the fragment may extend past the lifecycle of its views by design). This
 * makes callbacks a bit safer & more intuitive on when they get invoked. However, Dialog fragments
 * created by overwriting onCreateDialog do not have a view to which life cycles can be associated.
 * Instead we'd have to take an approach that uses onCreateView instead. This isn't entirely
 * desirable as it then means that we'd have to drive a lot of the dialog visual boilerplate
 * ourselves. In essence, I've come up with three options:
 *
 * 1) use onCreateView to get a view for which we can procure a lifecycle owner. Requires
 *      driving LnF, having UI related headaches (such as DPI adjustments and the like). etc.
 * 2) Use the fragment for the lifecycle fragment - requires being cognisant of when the callback
 *      should actually do anything.
 * 3) Pre-load any data for which we'd want the dialog fragment accessing in the view model / live
 *      data into the fragment at construction (using a static initializer / factory / etc.)
 *      then communicating user actions back out of the fragment via the view model with out ever
 *      observing values.
 *
 *  Option 3 is probably the best choice, however it'll necessitate the creation of parcelables and
 *  the like, so for now i'm going to test out option 2 and see how stable it is.
 */

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;

import com.mesalu.viv2.android_ui.R;

/**
 * Base class for other dialogfragments to extend: adds the `safeInvoke` methods
 * that can be used to wrap livedata related observer methods.
 */
public class LifeCycleCognizantDialogFragment extends DialogFragment {
    public interface CompoundConsumer <T> {
        void accept(T data, View view);
    }
    /**
     * Invokes `ifSafe` if the dialog is alive & well and a view with matching `id` is found
     * in the dialog. The view passed to `ifSafe` is the view found by `id`.
     * @param id
     * @param ifSafe
     */
    protected <T> Observer<T> cognizantObserver(@IdRes int id, CompoundConsumer<T> ifSafe) {
        return x -> {
            if (getDialog() != null) {
                View view = getDialog().findViewById(id);
                if (view != null) ifSafe.accept(x, view);
            }
        };
    }

    /**
     * As safeInvoke(id, ifSafe) except id is assumed to be R.id.container.
     *
     * Invokes `ifSafe` if the dialog is alive & well and a view with matching `id` is found
     * in the dialog. The view passed to `ifSafe` is the view found by `id`.
     * @param ifSafe
     */
    protected <T> Observer<T> cognizantObserver(CompoundConsumer<T> ifSafe) {
        return cognizantObserver(R.id.container, ifSafe);
    }
}
