package com.mesalu.viv2.android_ui.ui.overview;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;
import com.mesalu.viv2.android_ui.ui.login.LoginViewModel;
import com.mesalu.viv2.android_ui.ui.widgets.LedValueView;

import java.util.ArrayList;
import java.util.List;

public class PetReviewFragment extends Fragment {
    private PetReviewViewModel viewModel;

    public static PetReviewFragment newInstance() {
        return new PetReviewFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        viewModel = new ViewModelProvider(this).get(PetReviewViewModel.class);

        final ProgressBar progressBar = getView().findViewById(R.id.loading);
        progressBar.setVisibility(View.VISIBLE);

        viewModel.getIdObservable().observe(getViewLifecycleOwner(), idList -> {
            for (Integer i : idList) Log.d("PRF", "Got id: " + i.toString());

            // attach an adapter to the recycler view.
            Adapter adapter = new Adapter(idList);
            RecyclerView recycler = getView().findViewById(R.id.recycler_view);
            recycler.setAdapter(adapter);

            // hide the loading bar
            progressBar.setVisibility(View.GONE);
        });

        if (LoginRepository.getInstance().isLoggedIn())
            viewModel.fetchPetIdList();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private int petId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            petId = -1;
        }

        public synchronized void setPendingUpdate(int id) {
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
        }

        protected void fillNullSample() {
            for (int resId : new int[] { R.id.hg_view, R.id.hm_view,
                    R.id.mg_view, R.id.cg_view, R.id.cm_view }) {
                LedValueView ledView = itemView.findViewById(resId);
                ledView.setText(R.string.na_entry);
                ledView.setLedLevel(LedValueView.LedLevel.WARN);
            }
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
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        List<Integer> petIds;

        public Adapter(List<Integer> ids) {
            petIds = ids;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create a containing card.
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_pet_overview, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            // request requisite info from view model. The call in async
            // and may take time - as such when processing the result we need
            // to be sure that the context is still valid & that the view holder
            // hasn't been re-bound. (Luckily, all aspects are processed on UI thread
            // so we don't have to worry about all the nuances of concurrency)
            final int id = petIds.get(position);

            // set up the view holder to expect the incoming change:
            holder.setPendingUpdate(id);

            // TODO: remove old observer associated to this view holder.

            viewModel.getPreliminaryPetInfo(petIds.get(position),
                    PetReviewFragment.this,
                    new Observer<PreliminaryPetInfo>() {
                        @Override
                        public void onChanged(PreliminaryPetInfo preliminaryPetInfo) {
                            Log.d("PRFA", "observer's onChanged called");
                            holder.update(id, preliminaryPetInfo);
                        }
                    }
            );
        }

        @Override
        public int getItemCount() {
            return petIds.size();
        }
    }
}