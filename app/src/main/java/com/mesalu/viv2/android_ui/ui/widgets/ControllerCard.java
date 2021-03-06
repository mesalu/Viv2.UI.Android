package com.mesalu.viv2.android_ui.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.model.Environment;
import com.mesalu.viv2.android_ui.data.model.NodeController;

import java.util.ArrayList;
import java.util.List;

/**
 * Logic behind a ControllerCard (as seen in the Environment/Controller review fragment.
 *
 * The nested adapter logic for controller->env model classes made the hosting fragment
 * pretty cluttered, and definitely was not satisfying on the separation-of-concerns metric.
 * So it made more sense to create a custom widget to handle the instance
 * logic.
 */
public class ControllerCard extends MaterialCardView {

    /**
     * Callback interface for communicating actions this view can facilitate / emit.
     */
    public interface ControllerActionListener {

        /**
         * Called when a user takes an action (such as a menu action) to add a pet
         * to an environment. The handler should facilitate allowing the user to
         * select the pet & notifying the backend.
         *
         * @param controller the controller this card represents - and the one hosting the target
         *                   environment
         * @param environment the environment to add a pet to.
         */
        void onAddPetToEnv(NodeController controller, Environment environment);
    }

    NodeController controller;

    TextView guidView;
    TextView systemView;
    TextView versionView;
    Adapter envCardAdapter;

    ControllerActionListener listener;

    public ControllerCard(@NonNull Context context) {
        super(context);
        init();

    }

    public ControllerCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ControllerCard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setListener(ControllerActionListener listener) {
        this.listener = listener;
    }

    public void bind(NodeController controller) {
        this.controller = controller;

        guidView.setText(controller.getId());
        envCardAdapter.rebind(controller.getEnvironmentIds());
    }

    public void onEnvLoaded(int position, Environment env) {
        envCardAdapter.updatePosition(position, env);
    }

    public void onInhabitantNameLoaded(int position, String name) {
        envCardAdapter.updateName(position, name);
    }

    private void init() {
        inflate(getContext(), R.layout.card_controller_overview, this);

        guidView = findViewById(R.id.guid);
        systemView = findViewById(R.id.system);
        versionView = findViewById(R.id.version);

        // initialize an adapter.
        envCardAdapter = new Adapter();

        RecyclerView envRecycler = findViewById(R.id.env_container);
        envRecycler.setAdapter(envCardAdapter);

        // These are not yet implemented on the backend, so fill with defaults.
        systemView.setText(R.string.nyi_short);
        versionView.setText(R.string.nyi_short);
    }

    // helper class for smoothing out loaded/not-loaded environment states.
    class EnvironmentHolder {
        Environment env;
        String envId;   // always set - even while awaiting rest of env to load.
        String inhabitantName;

        EnvironmentHolder(String id) {
            envId = id;
        }
    }

    private class EnvCardHolder extends RecyclerView.ViewHolder {
        EnvCardHolder(View view) {
            super(view);
        }

        public void onBind(EnvironmentHolder modelHolder, @Nullable ControllerActionListener listener) {
            TextView tv = itemView.findViewById(R.id.guid);
            tv.setText(modelHolder.envId);

            if (modelHolder.env != null) {
                // populate all the sub fields.
                tv = itemView.findViewById(R.id.description);
                tv.setText(modelHolder.env.getDescription());

                tv = itemView.findViewById(R.id.model);
                tv.setText(modelHolder.env.getModel());
            }
            else {
                // reset subviews & set the loading indicator.

            }

            tv = itemView.findViewById(R.id.occupant_name);
            if (modelHolder.inhabitantName != null) {
                tv.setText(modelHolder.inhabitantName);
            }
            else {
                // reset & set loading indicator.
                tv.setText("");
            }

            // Bind buttons to actions in listener:
            ImageButton button = itemView.findViewById(R.id.card_main_button);
            if (listener != null && modelHolder.env != null) {
                button.setEnabled(true);
                button.setOnClickListener(v -> listener.onAddPetToEnv(controller, modelHolder.env));
            }
            else {
                button.setEnabled(false);
                button.setOnClickListener(v -> {});
            }
        }
    }


    private class Adapter extends RecyclerView.Adapter<EnvCardHolder> {
        List<EnvironmentHolder> modelHolders;

        public Adapter() {
            modelHolders = new ArrayList<>();
        }

        public void rebind(List<String> idList) {
            modelHolders = new ArrayList<>(idList.size());
            for (String id: idList) {
                modelHolders.add(new EnvironmentHolder(id));
            }
            notifyDataSetChanged();
        }

        public void updatePosition(int position, Environment env) {
            modelHolders.get(position).env = env;
            notifyItemChanged(position);
        }

        public void updateName(int position, String name) {
            modelHolders.get(position).inhabitantName = name;
            notifyItemChanged(position);
        }

        @NonNull
        @Override
        public EnvCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_env_overview, parent, false);
            return new EnvCardHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EnvCardHolder holder, int position) {
            holder.onBind(modelHolders.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return modelHolders.size();
        }
    }
}
