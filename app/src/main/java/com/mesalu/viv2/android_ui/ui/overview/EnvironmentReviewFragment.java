package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.model.NodeController;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentReviewFragment extends Fragment {

    private EnvironmentInfoViewModel mViewModel;

    public static EnvironmentReviewFragment newInstance() {
        return new EnvironmentReviewFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_environment_review, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(EnvironmentInfoViewModel.class);

        final RecyclerView recycler = requireView().findViewById(R.id.recycler_view);
        final ProgressBar progressBar = requireView().findViewById(R.id.loading);
        progressBar.setVisibility(View.VISIBLE);

        mViewModel.getFabSignal().observe(getViewLifecycleOwner(), event -> {
            if (event.consume()) {
                Snackbar snackbar = Snackbar
                        .make(requireView(), R.string.nyi_long, Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });

        mViewModel.getControllersObservable().observe(getViewLifecycleOwner(), nodeControllers -> {
            // compose an adapter and assign it to recycler view.
            Adapter adapter = new Adapter(nodeControllers);
            recycler.setAdapter(adapter);
            progressBar.setVisibility(View.GONE);
        });
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(NodeController controller) {
            // clear presentation elements.
            ListView containerView = itemView.findViewById(R.id.env_container);

            if (controller.getEnvironmentIds().size() == 0)
                containerView.setAdapter(null);
            else {
                // compose (perhaps even memoize? a list adapter.
            }

            TextView tv = itemView.findViewById(R.id.guid);
            tv.setText(controller.getId());
        }
    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {
        List<NodeController> controllers;

        public Adapter(List<NodeController> controllerList) {
            controllers = new ArrayList<>(controllerList);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View rootView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_controller_overview, parent, false);

            return new ViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(controllers.get(position));
        }

        @Override
        public int getItemCount() {
            return controllers.size();
        }
    }

}