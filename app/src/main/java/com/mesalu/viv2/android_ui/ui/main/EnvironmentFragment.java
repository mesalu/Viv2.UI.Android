package com.mesalu.viv2.android_ui.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.material.snackbar.Snackbar;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.ui.events.ChartTargetEvent;
import com.mesalu.viv2.android_ui.ui.main.data_entry.PetMigrationDialogFragment;
import com.mesalu.viv2.android_ui.ui.widgets.ControllerCard;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentFragment extends Fragment {

    private EnvironmentInfoViewModel envInfoViewModel;
    private PetInfoViewModel petInfoViewModel;
    private IChartTargetHandler chartTargetHandler;

    public static EnvironmentFragment newInstance() {
        return new EnvironmentFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (envInfoViewModel != null)
            // hide the chart if shown. (behavior not yet implemented.)
            chartTargetHandler.signalChartTarget(null, ChartTargetEvent.ViewModifier.HIDE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_environment_review, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        envInfoViewModel = provider.get(EnvironmentInfoViewModel.class);
        petInfoViewModel = provider.get(PetInfoViewModel.class);
        chartTargetHandler = provider.get(MainActivityViewModel.class); // TODO: better dependency management

        final RecyclerView recycler = requireView().findViewById(R.id.recycler_view);
        final ProgressBar progressBar = requireView().findViewById(R.id.loading);
        progressBar.setVisibility(View.VISIBLE);

        envInfoViewModel.getFabSignal().observe(getViewLifecycleOwner(), event -> {
            if (event.consume()) {
                Snackbar snackbar = Snackbar
                        .make(requireView(), R.string.nyi_long, Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });

        envInfoViewModel.getControllersObservable().observe(getViewLifecycleOwner(), nodeControllers -> {
            // compose an adapter and assign it to recycler view.
            Adapter adapter = new Adapter(nodeControllers);
            recycler.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);
        });
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        // used to ensure only the most recent binding acts on the view
        String pendingId;
        ControllerCard cardView;

        ViewHolder(ControllerCard cardView) {
            super(cardView);

            // to avoid constantly recasting this.itemView:
            this.cardView = cardView;
        }

        public void bind(NodeController controller) {
            pendingId = controller.getId();
            cardView.bind(controller);
        }

        public void onEnvironmentLoaded(String controllerId, int position, Environment env) {
            // check if we got rebound before data transaction finished.
            if (controllerId.compareTo(pendingId) != 0) return;

            cardView.onEnvLoaded(position, env);
        }

        public void onPetNameLoaded(String controllerId, int position, String name) {
            if (controllerId.compareTo(pendingId) != 0) return;
            cardView.onInhabitantNameLoaded(position, name);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        List<NodeController> controllers;

        public Adapter(List<NodeController> controllerList) {
            controllers = new ArrayList<>(controllerList);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ControllerCard card = new ControllerCard(parent.getContext());

            card.setListener(((controller, environment) -> {
                // Eventually I want to swap this to a context action menu
                // but for now, just popping up (yet another) dialog should be fine.
                PetMigrationDialogFragment.create(controller, environment)
                        .show(getChildFragmentManager(), PetMigrationDialogFragment.TAG);
            }));

            card.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final NodeController controller = controllers.get(position);
            holder.bind(controller);

            // Set up callbacks for populating environment cards.
            for (int i = 0; i < controller.getEnvironmentIds().size(); i++) {
                final String envId = controller.getEnvironmentIds().get(i);

                int finalI = i;
                envInfoViewModel.getEnvironmentObservable(envId).observe(
                        EnvironmentFragment.this.getViewLifecycleOwner(),
                        environment -> {
                            holder.onEnvironmentLoaded(controller.getId(), finalI, environment);

                            // now try to get the pet name:
                            if (environment.getOccupantId() > 0) {
                                LiveData<Pet> petObservable = petInfoViewModel.getPetObservable(environment.getOccupantId());
                                petInfoViewModel.observeOnce(petObservable, getViewLifecycleOwner(), pet ->
                                        holder.onPetNameLoaded(controller.getId(), finalI,pet.getName())
                                );
                            }
                        });
            }
        }

        @Override
        public int getItemCount() {
            return controllers.size();
        }
    }
}