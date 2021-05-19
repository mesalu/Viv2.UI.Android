package com.mesalu.viv2.android_ui.ui.overview;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
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
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.ui.events.SimpleEvent;
import com.mesalu.viv2.android_ui.ui.overview.data_entry.PetEntryDialogFragment;
import com.mesalu.viv2.android_ui.ui.widgets.LedValueView;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.List;

public class PetReviewFragment extends Fragment {
    private PetInfoViewModel viewModel;
    private Adapter recyclerAdapter;
    private ActionMode.Callback actionModeCallback;
    private ActionMode activeActionMode;

    public static PetReviewFragment newInstance() {
        return new PetReviewFragment();
    }


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
                        Log.d("PRF", "On delete for pet where id == " + petId);
                        mode.finish();
                    }
                }
                else if (item.getItemId() == R.id.action_chart) {
                    // Launch a fragment to display history chart for selected pet.
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
            }
        };

        return inflater.inflate(R.layout.fragment_pet_review, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("PRF", "onResume");
        // catch a side case where on app launch onActivityCreated is called prior
        // to login being completed - therefore requisite info is never obtained.
        if (viewModel.petIdListLoaded()) {
            Log.d("PRF", "pet list already loaded");
        }
        else {
            Log.w("PRF", "pet list not loaded");
            if (LoginRepository.getInstance().isLoggedIn())
                viewModel.fetchPetIdList();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PetInfoViewModel.class);

        final ProgressBar progressBar = getView().findViewById(R.id.loading);
        progressBar.setVisibility(View.VISIBLE);

        viewModel.getIdsObservable().observe(getViewLifecycleOwner(), idList -> {
            for (Integer i : idList) Log.d("PRF", "Got id: " + i.toString());

            // attach an adapter to the recycler view.
            recyclerAdapter = new Adapter(idList);
            RecyclerView recycler = getView().findViewById(R.id.recycler_view);
            recycler.setAdapter(recyclerAdapter);

            // hide the loading bar
            progressBar.setVisibility(View.GONE);
        });

        viewModel.getFabSignal().observe(getViewLifecycleOwner(), event -> {
            if (event.consume()) {
                // start a data entry dialog?
                Log.d("PRF", "Fab handler notified");
                new PetEntryDialogFragment()
                        .show(getChildFragmentManager(), PetEntryDialogFragment.TAG);
            }
        });

        viewModel.getRefreshSignal().observe(getViewLifecycleOwner(), event -> {
            if (event.consume()) {
                Log.d("PRF", "Refresh handler notified");

                if (viewModel.petIdListLoaded()) {
                    viewModel.refreshAllPreliminaryPetInfo();
                }
            }
        });

        if (LoginRepository.getInstance().isLoggedIn())
            viewModel.fetchPetIdList();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        interface ViewActionListener {
            boolean onLongClick(View itemView, int position);
            void onClick(View itemView, int position);
        }

        int petId;
        ViewActionListener actionListener;

        Observer<SimpleEvent> uiUpdateObserver;
        Observer<PreliminaryPetInfo> petInfoObserver;

        ZonedDateTime lastSampleTime;

        public ViewHolder(@NonNull View itemView, @NonNull ViewActionListener listener) {
            super(itemView);
            petId = -1;
            actionListener = listener;

            itemView.setOnLongClickListener(v -> {
                // TODO: getAdapterPosition is deprecated according to current docs,
                //      will need to swap to getAbsoluteAdapterPosition
                return actionListener.onLongClick(v, getAdapterPosition());
            });

            itemView.setOnClickListener(v -> actionListener.onClick(v, getAdapterPosition()));
        }

        public synchronized void setPendingUpdate(int id, Observer<PreliminaryPetInfo> petInfoObserver) {
            this.petInfoObserver = petInfoObserver;
            petId = id;
        }

        public void update(int id, PreliminaryPetInfo data) {
            if (id != petId) return; // got re-bound before callback finished

            // update view with content.
            TextView textView = itemView.findViewById(R.id.name_view);
            textView.setText(data.getPet().getName());

            textView = itemView.findViewById(R.id.species_name_view);
            textView.setText(data.getPet().getSpecies().getName());

            textView = itemView.findViewById(R.id.morph_view);
            textView.setText(data.getPet().getMorph());

            if (data.getSample() == null) fillNullSample(); // done here.
            else fillSample(data.getSample());
        }

        protected void fillSample(EnvDataSample sample) {
            lastSampleTime = sample.getCaptureTime();

            // TODO: eventually temp-windows will be bundled in with
            //      pet info, use that for setting Led tone.

            LedValueView ledView = itemView.findViewById(R.id.hg_view);
            float val = centigradeToFahrenheit(sample.getHotGlass());
            setLed(ledView, 88f, 92f, (float) val);
            ledView.setText(val);

            ledView = itemView.findViewById(R.id.hm_view);
            val = centigradeToFahrenheit(sample.getHotMat());
            setLed(ledView, 88f, 92f, (float) val);
            ledView.setText(val);

            ledView = itemView.findViewById(R.id.mg_view);
            val = centigradeToFahrenheit(sample.getMidGlass());
            setLed(ledView, 80f, 90f, (float) val);
            ledView.setText(val);

            ledView = itemView.findViewById(R.id.cg_view);
            val = centigradeToFahrenheit(sample.getColdGlass());
            setLed(ledView, 78f, 82f, (float) val);
            ledView.setText(val);

            ledView = itemView.findViewById(R.id.cm_view);
            val = centigradeToFahrenheit(sample.getColdMat());
            setLed(ledView, 78f, 82f, (float) val);
            ledView.setText(val);

            updateTimeStamp();
        }

        protected void fillNullSample() {
            for (int resId : new int[] { R.id.hg_view, R.id.hm_view,
                    R.id.mg_view, R.id.cg_view, R.id.cm_view }) {
                LedValueView ledView = itemView.findViewById(resId);
                ledView.setText(R.string.na_entry);
                ledView.setLedLevel(LedValueView.LedLevel.WARN);
            }

            // Clear latest sample time, so that the ui update event
            // doesn't repopulate the field.
            lastSampleTime = null;
            TextView tv = itemView.findViewById(R.id.sample_age_view);
            tv.setText("");
        }

        private static float centigradeToFahrenheit(double centigrade) {
            return (float) ((centigrade * 1.8) + 32);
        }

        public static void setLed(@NonNull LedValueView view, float a, float b, float x) {
            float range = b - a;
            float tolerance = range / 10;
            if (Math.abs(a - x) < tolerance || Math.abs(b - x) < tolerance)
                view.setLedLevel(LedValueView.LedLevel.WARN);
            else if (a < x && x < b)
                view.setLedLevel(LedValueView.LedLevel.GOOD);
            else
                view.setLedLevel(LedValueView.LedLevel.BAD);
        }

        private String makeAgeStringLabel(Context context, Period major, Duration minor) {
            // Scan from largest units to smallest for the first non-0 value.
            @StringRes int unitStringRes;
            long value;

            if (!major.isZero()) {
                if (major.getYears() > 0) {
                    value = major.getYears();
                    unitStringRes = R.string.time_unit_year;
                }
                else if (major.getMonths() > 0) {
                    value = major.getMonths();
                    unitStringRes = R.string.time_unit_month;
                }
                else { // days must have value
                    value = major.getDays();
                    // check if we should up to weeks.
                    long weeks = value / 7;
                    if (weeks > 0) {
                        value = weeks;
                        unitStringRes = R.string.time_unit_week;
                    }
                    else unitStringRes = R.string.time_unit_day;
                }
            }
            else {
                // the major time span was empty:
                value = minor.getSeconds();
                unitStringRes = R.string.time_lt_minute;

                long minutes = value / 60;
                long hours = value / 3600;

                if (hours > 0) {
                    value = hours;
                    unitStringRes = R.string.time_unit_hour;
                }
                else if (minutes > 0) {
                    value = minutes;
                    unitStringRes = R.string.time_unit_minute;
                }
            }

            if (unitStringRes == R.string.time_lt_minute)
                return context.getResources().getString(unitStringRes);

            return context.getResources().getString(R.string.time_base_format,
                    value,
                    context.getResources().getString(unitStringRes),
                    (value > 1) ? "s" : "");
        }

        public void updateTimeStamp() {
            // compute a delta between now and when the sample was captured
            // generate a human readable (localized) string for expressing the delta
            if (lastSampleTime == null) return; // update fired & processed before holder ready.

            ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);

            Log.d("PRFA", "Now: " + now.toString() + ", Sample: " + lastSampleTime.toString());
            Duration minorSpan = Duration.between(lastSampleTime, now);
            Period majorSpan = Period.between(lastSampleTime.toLocalDate(),
                    now.toLocalDate());

            TextView tv = itemView.findViewById(R.id.sample_age_view);
            tv.setText(makeAgeStringLabel(itemView.getContext(), majorSpan, minorSpan));
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        List<Integer> petIds;
        int selectedItemPosition;

        public Adapter(List<Integer> ids) {
            petIds = ids;
            selectedItemPosition = RecyclerView.NO_POSITION;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create a containing card.
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_pet_overview, parent, false);

            return new ViewHolder(view, new ViewHolder.ViewActionListener() {
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
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            if (holder.uiUpdateObserver == null) {
                // give it a new observer
                holder.uiUpdateObserver = event ->
                    holder.updateTimeStamp();

                viewModel.getUiUpdateSignal().observe(PetReviewFragment.this.getViewLifecycleOwner(),
                        holder.uiUpdateObserver);
            }

            // request requisite info from view model. The call in async
            // and may take time - as such when processing the result we need
            // to be sure that the context is still valid & that the view holder
            // hasn't been re-bound. (Luckily, all aspects are processed on UI thread
            // so we don't have to worry about all the nuances of concurrency)
            final int id = petIds.get(position);

            Observer<PreliminaryPetInfo> observer = preliminaryPetInfo ->
                    holder.update(id, preliminaryPetInfo);
            LiveData<PreliminaryPetInfo> observable = viewModel.getPreliminaryInfoObservable(id, true);


            // set up the view holder to expect the incoming change:
            if (holder.petInfoObserver != null) {
                // Prevent the build up of out dated observers.
                observable.removeObserver(observer);
            }

            holder.setPendingUpdate(id, observer);
            holder.itemView.setSelected(position == selectedItemPosition);

            observable.observe(PetReviewFragment.this.getViewLifecycleOwner(), observer);
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);

            if (holder.petInfoObserver != null) {
                viewModel.getPreliminaryInfoObservable(holder.petId).removeObserver(holder.petInfoObserver);
                holder.petInfoObserver = null;
            }
            if (holder.uiUpdateObserver != null) {
                viewModel.getUiUpdateSignal().removeObserver(holder.uiUpdateObserver);
                holder.uiUpdateObserver = null;
            }
        }

        @Override
        public int getItemCount() {
            return petIds.size();
        }

        /**
         * returns the ID of the pet corresponding to the selected item - if any.
         * If no view is selected then -1 is returned.
         * @return Id of the pet selected by the user.
         */
        public int getSelectedPetId() {
            if (selectedItemPosition != RecyclerView.NO_POSITION)
                return petIds.get(selectedItemPosition);
            return -1;
        }

        public void clearSelectedItem() {
            notifyItemChanged(selectedItemPosition);
            selectedItemPosition = RecyclerView.NO_POSITION;
        }
    }
}