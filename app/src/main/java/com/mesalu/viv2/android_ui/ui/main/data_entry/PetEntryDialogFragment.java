package com.mesalu.viv2.android_ui.ui.main.data_entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.Species;
import com.mesalu.viv2.android_ui.ui.main.nav_fragments.PetInfoViewModel;

import java.util.ArrayList;
import java.util.List;

public class PetEntryDialogFragment extends LifeCycleCognizantDialogFragment {
    // for fragment manager transactions.
    public static final String TAG = "PetEntryDialog";

    private PetInfoViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(PetInfoViewModel.class);
        final View rootView = getLayoutInflater().inflate(R.layout.dialog_create_pet, null);

        // initialize components of rootView here.
        viewModel.getSpeciesObservable().observe(this,
                cognizantObserver((data, view) -> {
                    Spinner spinner = view.findViewById(R.id.species_spinner);
                    attachAdapter(spinner, data);
                }));

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title_pet_entry)
                .setView(rootView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    extractAndSubmit(rootView);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {})
                .create();
    }

    /**
     * Composes & attaches a spinner adapter to the given spinner instance.
     */
    private void attachAdapter(Spinner spinner, List<Species> species) {
        // convert the species list into a SpeciesSpinnerItem list:
        ArrayList<SpeciesSpinnerItem> l = new ArrayList<>(species.size());
        for (Species x : species) {
            l.add(new SpeciesSpinnerItem(x));
        }

        // Set up spinner's adapter.
        ArrayAdapter<SpeciesSpinnerItem> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, l);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
    }

    /**
     * extracts the components from the specified view (assumed to be an inflation of
     * R.layout.dialog_create_pet) and forwards the info for pet-creation in the backend
     * interface layer.
     */
    private void extractAndSubmit(View containerView) {
        Pet pet = new Pet();

        EditText et = containerView.findViewById(R.id.name_entry);
        pet.setName(et.getText().toString());

        et = containerView.findViewById(R.id.morph_entry);
        pet.setMorph(et.getText().toString());

        Spinner spinner = containerView.findViewById(R.id.species_spinner);
        Object selection = spinner.getSelectedItem();
        if (!(selection instanceof SpeciesSpinnerItem)) throw new RuntimeException("whoopsies?");
        pet.setSpecies(((SpeciesSpinnerItem) selection).species);

        // pass on down to view-model/repo layer:
        viewModel.submitNewPet(pet);
    }

    // Wraps a species model for us as a simple spinner item.
    private static class SpeciesSpinnerItem {
        Species species;

        public SpeciesSpinnerItem(@NonNull Species species) {
            this.species = species;
        }

        @NonNull
        @Override
        public String toString() {
            return species.getName();
        }
    }
}
