package com.mesalu.viv2.android_ui.ui.overview.data_entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;
import com.mesalu.viv2.android_ui.ui.overview.EnvironmentInfoViewModel;
import com.mesalu.viv2.android_ui.ui.overview.PetInfoViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to specify a 'pet migration' - assigning a pet to an environment.
 */
public class PetMigrationDialogFragment extends LifeCycleCognizantDialogFragment {
    public static String TAG = "PetMigrationDialog";
    private static String ARG_ENV_ID = "EnvironmentId";
    private static String ARG_CONTROLLER_ID = "ControllerId";

    private PetInfoViewModel petViewModel;
    private EnvironmentInfoViewModel envViewModel;


    public static PetMigrationDialogFragment create(NodeController controller, Environment environment) {
        PetMigrationDialogFragment fragment = new PetMigrationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONTROLLER_ID, controller.getId());
        args.putString(ARG_ENV_ID, environment.getId());

        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        petViewModel = provider.get(PetInfoViewModel.class);
        envViewModel = provider.get(EnvironmentInfoViewModel.class);

        // compose the main view
        final View view = getLayoutInflater().inflate(R.layout.dialog_migrate_pet, null);

        // set up callbacks to populate the view when ready
        envViewModel.getEnvListObservable().observe(
                this, cognizantObserver(R.id.env_spinner, (data, v) -> {
                    // build an adapter from the list `data` & assign it to v
                    Spinner spinner = (Spinner) v;

                    List<EnvSpinnerItem> items = new ArrayList<>(data.size());
                    data.forEach(env -> items.add(new EnvSpinnerItem(env)));

                    ArrayAdapter<EnvSpinnerItem> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, items);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spinner.setAdapter(adapter);
                }));

        // build a dialog
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_migrate_pet_title)
                .setMessage(R.string.dialog_migrate_pet_message)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Log.d("PMDF", "Controller: " + getArguments().getString(ARG_CONTROLLER_ID));
                    Log.d("PMDF", "Env: " + getArguments().getString(ARG_ENV_ID));
                })
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {}))
                .create();
    }

    private static class EnvSpinnerItem {
        Environment env;

        EnvSpinnerItem(@NonNull Environment env) {
            this.env = env;
        }

        @Override
        public String toString() {
            return env.getId();
        }
    }
}
