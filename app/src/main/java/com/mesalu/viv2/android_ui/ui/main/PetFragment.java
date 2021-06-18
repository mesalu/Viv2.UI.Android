package com.mesalu.viv2.android_ui.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;
import com.mesalu.viv2.android_ui.ui.main.data_entry.PetEntryDialogFragment;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PetFragment extends Fragment {
    static int IMAGE_CAPTURE_REQUEST_CODE = 1;

    private PetInfoViewModel viewModel;
    private Adapter recyclerAdapter;
    private ActionMode.Callback actionModeCallback;
    private ActionMode activeActionMode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // as good a time as any to instantiate actionModeCallback:
        actionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.overview_contextual_actions, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // not yet sure what we'll need to do here.
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // handle items:
                if (item.getItemId() == R.id.action_delete) {
                    if (recyclerAdapter != null) {
                        int petId = recyclerAdapter.getSelectedPetId();
                        Log.d("PetFragment", "On delete for pet where id == " + petId);
                        mode.finish();
                    }
                }
                else if (item.getItemId() == R.id.action_chart) {
                    // Signal the main activity that it should show & configure the chart display
                    viewModel.setChartTargetAs(new ChartTarget(recyclerAdapter.getSelectedPetId(),
                            ChartTarget.TargetType.Pet));
                }
                else return false; // did not handle the item.

                // we handled the item:
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                activeActionMode = null;

                // recycler is set in live data call back, need to confirm atomicity & which thread
                // those callbacks execute on.
                if (recyclerAdapter != null) recyclerAdapter.clearSelectedItem();

                viewModel.setChartTargetAs(null); // Declare we're done with the chart
            }
        };

        return inflater.inflate(R.layout.fragment_pet_review, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("PetFragment", "onResume");
        // catch a side case where on app launch onActivityCreated is called prior
        // to login being completed - therefore requisite info is never obtained.
        if (viewModel.petIdListLoaded()) {
            Log.d("PetFragment", "pet list already loaded");
        }
        else {
            Log.w("PetFragment", "pet list not loaded");
            if (LoginRepository.getInstance().isLoggedIn())
                viewModel.fetchPetIdList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (activeActionMode != null) activeActionMode.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
            // Handle image result.
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PetInfoViewModel.class);

        final ProgressBar progressBar = getView().findViewById(R.id.loading);
        progressBar.setVisibility(View.VISIBLE);

        viewModel.getIdsObservable().observe(getViewLifecycleOwner(), idList -> {
            for (Integer i : idList) Log.d("PetFragment", "Got id: " + i.toString());

            // attach an adapter to the recycler view.
            RecyclerView recycler = getView().findViewById(R.id.recycler_view);
            Adapter adapter = (Adapter) recycler.getAdapter();
            if (adapter != null)
                adapter.updateIdList(idList);
            else {
                recyclerAdapter = new Adapter(idList);
                recycler.setAdapter(recyclerAdapter);
            }

            // hide the loading bar
            progressBar.setVisibility(View.GONE);
        });

        viewModel.getFabSignal().observe(getViewLifecycleOwner(), event -> {
            if (event.consume()) {
                // start a data entry dialog?
                Log.d("PetFragment", "Fab handler notified");
                new PetEntryDialogFragment()
                        .show(getChildFragmentManager(), PetEntryDialogFragment.TAG);
            }
        });

        viewModel.getRefreshSignal().observe(getViewLifecycleOwner(), event -> {
            if (event.consume()) {
                Log.d("PetFragment", "Refresh handler notified");
                if (activeActionMode != null) {
                    activeActionMode.finish();
                }

                progressBar.setVisibility(View.VISIBLE);

                if (viewModel.petIdListLoaded()) {
                    viewModel.refreshAllPetInfo();
                }
            }
        });

        if (LoginRepository.getInstance().isLoggedIn())
            viewModel.fetchPetIdList();
    }

    /**
     * Bridges the gap between progress-aware (e.g. async loading) view
     * and progress unaware model classes.
     */
    private static class ModelItem {
        int id;
        Pet model;
        boolean awaitingUpdate;

        public ModelItem(int id) {
            this.id = id;
        }
    }

    private class Adapter extends RecyclerView.Adapter<PetCardViewHolder> {
        List<ModelItem> petItems;
        HashSet<Integer> petIds;
        int selectedItemPosition;

        public Adapter(List<Integer> idList) {
            updateIdList(idList);
            selectedItemPosition = RecyclerView.NO_POSITION;
        }

        public void updateIdList(List<Integer> idList) {
            // TODO: avoid replacing modelitem list if the new idList is no different than
            //       current IDs (this will preserve UI state.)

            HashSet<Integer> ids = new HashSet<>(idList);
            if (ids.equals(petIds)) {
                petItems.forEach(item -> item.awaitingUpdate = true);
                notifyDataSetChanged();
                return;
            }

            petItems = idList.stream()
                    .map(ModelItem::new)
                    .collect(Collectors.toList());
            petIds = ids;

            notifyDataSetChanged();

            // TODO: build the list of items as deltas from previous list (if any) then notify
            //      only on migrated sections.
        }

        @NonNull
        @Override
        public PetCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create a containing card.
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_pet_overview, parent, false);

            return new PetCardViewHolder(view, new PetCardViewHolder.ViewActionListener() {
                @Override
                public boolean onLongClick(View itemView, int position) {
                    // if not in action mode, start it
                    if (activeActionMode != null) return false;

                    // start action mode & select the item view.
                    activeActionMode = requireActivity().startActionMode(actionModeCallback);

                    // select the card, clear the old selection first if applicable
                    int oldSelection = selectedItemPosition;
                    selectedItemPosition = position;

                    if (oldSelection != RecyclerView.NO_POSITION)
                        notifyItemChanged(oldSelection);

                    itemView.setSelected(true);
                    notifyItemChanged(selectedItemPosition);

                    return true;
                }

                @Override
                public void onClick(View itemView, int position) {
                    if (activeActionMode == null) return; // not in action mode, nothing to do.

                    int oldPosition = selectedItemPosition;
                    selectedItemPosition = position;
                    notifyItemChanged(oldPosition);
                    notifyItemChanged(selectedItemPosition);

                    // Send a message to retarget the chart - if its shown. (e.g., don't show it
                    // if the user hasn't explicitly opened it.)
                    ChartTarget newTarget = new ChartTarget(petItems.get(position).id,
                            ChartTarget.TargetType.Pet);
                    viewModel.setChartTargetAs(newTarget, false);
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull final PetCardViewHolder holder, int position) {
            if (holder.uiUpdateObserver == null) {
                // give it a new observer
                holder.uiUpdateObserver = event ->
                    holder.updateTimeStamp();

                viewModel.getUiUpdateSignal().observe(PetFragment.this.getViewLifecycleOwner(),
                        holder.uiUpdateObserver);
            }

            // request requisite info from view model. The call in async
            // and may take time - as such when processing the result we need
            // to be sure that the context is still valid & that the view holder
            // hasn't been re-bound. (Luckily, all aspects are processed on UI thread
            // so we don't have to worry about all the nuances of concurrency)
            final ModelItem item = petItems.get(position);
            boolean shouldRefresh = holder.petId != item.id && activeActionMode == null;
            item.awaitingUpdate = item.awaitingUpdate || shouldRefresh;

            holder.setProfileImage(item.id);
            holder.showProgressIndicators(item.awaitingUpdate);

            Observer<Pet> observer = petInfo -> {
                // hide the progress indicator if applicable (going from no data to data, or from
                // old (cached) data to fresh data.
                if (petInfo != item.model) {
                    item.awaitingUpdate = false;
                    holder.showProgressIndicators(false);
                }

                item.model = petInfo;
                holder.update(item.id, petInfo);
            };

            // Get the observable - request new data if plausibly stale. (e.g., not a select-notification)
            LiveData<Pet> observable =
                    viewModel.getPetObservable(item.id, shouldRefresh);


            // set up the view holder to expect the incoming change:
            if (holder.petInfoObserver != null) {
                // Prevent the build up of out dated observers.
                observable.removeObserver(observer);
            }

            holder.setPendingUpdate(item.id, observer);
            holder.itemView.setSelected(position == selectedItemPosition);

            observable.observe(PetFragment.this.getViewLifecycleOwner(), observer);
        }

        @Override
        public void onViewRecycled(@NonNull PetCardViewHolder holder) {
            super.onViewRecycled(holder);

            if (holder.petInfoObserver != null) {
                viewModel.getPetObservable(holder.petId).removeObserver(holder.petInfoObserver);
                holder.petInfoObserver = null;
            }
            if (holder.uiUpdateObserver != null) {
                viewModel.getUiUpdateSignal().removeObserver(holder.uiUpdateObserver);
                holder.uiUpdateObserver = null;
            }
        }

        @Override
        public int getItemCount() {
            return petItems.size();
        }

        /**
         * returns the ID of the pet corresponding to the selected item - if any.
         * If no view is selected then -1 is returned.
         * @return Id of the pet selected by the user.
         */
        public int getSelectedPetId() {
            if (selectedItemPosition != RecyclerView.NO_POSITION)
                return petItems.get(selectedItemPosition).id;
            return -1;
        }

        public void clearSelectedItem() {
            notifyItemChanged(selectedItemPosition);
            selectedItemPosition = RecyclerView.NO_POSITION;
        }
    }
}
