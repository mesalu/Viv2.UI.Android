package com.mesalu.viv2.android_ui.ui.main;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.ProfileImageRepository;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.ui.events.SimpleEvent;
import com.mesalu.viv2.android_ui.ui.widgets.LedValueView;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

class PetCardViewHolder extends RecyclerView.ViewHolder {
    interface ViewActionListener {
        boolean onLongClick(View itemView, int position);
        void onClick(View itemView, int position);
    }

    int petId;
    ViewActionListener actionListener;

    Observer<SimpleEvent> uiUpdateObserver;
    Observer<Pet> petInfoObserver;

    ZonedDateTime lastSampleTime;

    public PetCardViewHolder(@NonNull View itemView, @NonNull ViewActionListener listener) {
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

    public void showProgressIndicators(boolean show) {
        itemView.findViewById(R.id.progress_circular)
                .setVisibility((show) ? View.VISIBLE : View.GONE);
    }

    public synchronized void setPendingUpdate(int id, Observer<Pet> petInfoObserver) {
        this.petInfoObserver = petInfoObserver;
        petId = id;
    }

    public void update(int id, Pet data) {
        if (id != petId) return; // got re-bound before callback finished

        // update view with content.
        TextView textView = itemView.findViewById(R.id.name_view);
        textView.setText(data.getName());

        textView = itemView.findViewById(R.id.species_name_view);
        textView.setText(data.getSpecies().getName());

        textView = itemView.findViewById(R.id.morph_view);
        textView.setText(data.getMorph());

        if (data.getLatestSample() == null) fillNullSample(); // done here.
        else fillSample(data.getLatestSample());
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
            ledView.setLedLevel(LedValueView.LedLevel.GOOD);
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

    /**
     * Sets the level of the LED in accordance to x w.r.t the range [a, b]
     * @param view the LedValueView to operate on
     * @param a low end of the range.
     * @param b high end of the range.
     * @param x given measurement
     */
    public static void setLed(@NonNull LedValueView view, float a, float b, float x) {
        if (x < a)
            view.setLedLevel(LedValueView.LedLevel.LOW);
        else if (a < x && x < b)
            view.setLedLevel(LedValueView.LedLevel.GOOD);
        else
            view.setLedLevel(LedValueView.LedLevel.HIGH);
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

        Duration minorSpan = Duration.between(lastSampleTime, now);
        Period majorSpan = Period.between(lastSampleTime.toLocalDate(),
                now.toLocalDate());

        TextView tv = itemView.findViewById(R.id.sample_age_view);
        tv.setText(makeAgeStringLabel(itemView.getContext(), majorSpan, minorSpan));
    }

    protected void setProfileImage(int petId) {
        // Use picasso to acquire, transform and apply the profile image.
        ImageView imageView = itemView.findViewById(R.id.profile_image);
        ProfileImageRepository.getInstance().getPetImage(petId, imageView);
    }
}
