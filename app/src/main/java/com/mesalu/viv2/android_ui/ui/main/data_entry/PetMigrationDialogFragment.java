package com.mesalu.viv2.android_ui.ui.main.data_entry;

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
import com.mesalu.viv2.android_ui.data.PetInfoRepository;
import com.mesalu.viv2.android_ui.data.Result;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.ui.main.EnvironmentInfoViewModel;
import com.mesalu.viv2.android_ui.ui.main.PetInfoViewModel;

import java.util.function.Function;
import java.util.stream.Collectors;

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

                    ArrayAdapter<SpinnerItem<Environment>> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            data.stream().map(environment -> new SpinnerItem<>(environment, Environment::getId))
                                    .collect(Collectors.toList()));

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    // TODO: set default selected value to entry matching ARG_ENV_ID

                    spinner.setAdapter(adapter);
                }));

        petViewModel.getPetListObservable().observe(
                this,
                cognizantObserver(R.id.pet_spinner, (data, v) -> {
                    // same drill, compose an adapter & assign to the spinner
                    // DRY principle be like :(
                    Spinner spinner = (Spinner) v;

                    ArrayAdapter<SpinnerItem<Pet>> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            data.stream().map(pet -> new SpinnerItem<>(pet, Pet::getName))
                                    .collect(Collectors.toList()));

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    // TODO: set default pet entry to the pet already contained by env specified by
                    //       ARG_ENV_ID, if any.

                    spinner.setAdapter(adapter);
                }));

        // build a dialog
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_migrate_pet_title)
                .setMessage(R.string.dialog_migrate_pet_message)
                .setView(view)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    // extract selected pet & env:
                    Pet pet;
                    Environment env;

                    Spinner spinner = view.findViewById(R.id.pet_spinner);
                    pet = ((SpinnerItem<Pet>) spinner.getSelectedItem()).itemInstance;

                    spinner = view.findViewById(R.id.env_spinner);
                    env = ((SpinnerItem<Environment>) spinner.getSelectedItem()).itemInstance;


                    // Currently the ViewModels don't really support getting data updates unless
                    // they themselves instigate the update. As such, getting both models to be
                    // aware of the migration will be a jank-fest, so until a better flow is in
                    // position there - we'll bypass the view models and request updates from
                    // the view models on the call back. This will be less expensive when the
                    // repositories support caching.
                    PetInfoRepository.getInstance().migratePetToEnv(pet, env,
                            result -> {
                                if (result instanceof Result.Success) {
                                    Log.d("PFDM", "Migrated pet: " + pet.getName() + "!");
                                    envViewModel.updateEnvironment(env.getId());
                                    petViewModel.updatePetById(pet.getId());
                                }
                                else {
                                    // handle error (notify user, dismiss dialog if we manage
                                    // to keep it around, etc.)
                                    Log.e("PFDM", "Call failed.");
                                }
                            });

                    // TODO: delay closing of dialog for pass/fail?
                })
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {}))
                .create();
    }

    private static class SpinnerItem<TModel> {
        TModel itemInstance;
        Function<TModel, String> converter;

        SpinnerItem(@NonNull TModel item, @NonNull Function<TModel, String> converter) {
            itemInstance = item;
            this.converter = converter;
        }

        @Override
        public String toString() {
            return converter.apply(itemInstance);
        }
    }
}
