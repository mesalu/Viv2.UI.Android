package com.mesalu.viv2.android_ui.ui.charting;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.mesalu.viv2.android_ui.R;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A fragment that holds a chart view and related controls. The chart displays lines generated
 * from EnvDataSamples.
 */
public class ChartFragment extends Fragment {
    public static final String MGR_TAG = "ChatFragment";
    private SampleViewModel viewModel;

    private LineChart lineChart;
    private RadioGroup radioGroup;
    private final LineDataSet[] lineDataSets;
    private final boolean[] dataPending;
    private float timeScaleDivisor;

    // TODO: persist these through life cycle changes.
    private ChartTarget currentTarget;
    private Instant dataStart;

    public ChartFragment() {
        // Required empty public constructor
        int numZones = SampleZone.values().length;
        lineDataSets = new LineDataSet[numZones];

        // TODO: Idempotency tokens on data requests - prevent long awaiting tasks from
        //       interfering with view state if the user swaps away from the requested
        //       time frame.
        dataPending = new boolean[numZones];
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chart, container, false);
        lineChart = view.findViewById(R.id.chart);
        radioGroup = view.findViewById(R.id.radio_group);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDefaultState(view);
        initChart();
        bindButtonActions(view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Acquire a handle on the view model.
        viewModel = new ViewModelProvider(this).get(SampleViewModel.class);

        // listen to the line data observables.
        for (SampleZone zone : SampleZone.values()) {
            viewModel.getLineData(zone).observe(getViewLifecycleOwner(), data -> onLineDataUpdate(zone, data));
        }
    }

    /**
     * Resets the data target (the entity for which samples have been loaded)
     * and reverts the presentation to a default state (e.g., minimum time-range selected).
     * @param target a chart target indicating the entity for which data should be accessed.
     */
    public void resetToTarget(ChartTarget target) {
        setDefaultState(requireView());
        notifyModel(target, rangeStart(radioGroup.getCheckedRadioButtonId()));
    }

    private void notifyModel(ChartTarget target, Instant dataRangeStart) {
        // do some member up-keeping.
        currentTarget = target;
        dataStart = dataRangeStart;

        // show the progress bar while we await a result.
        requireView().findViewById(R.id.progress_circular).setVisibility(View.VISIBLE);

        // mark that we're waiting for new data across all zones.
        for (SampleZone zone : SampleZone.values())
            dataPending[zone.ordinal()] = true;

        if (target.targetType == ChartTarget.TargetType.PET)
            viewModel.setDataTarget((Integer) target.id, dataRangeStart, Instant.now());

        else if (target.targetType == ChartTarget.TargetType.ENVIRONMENT)
            viewModel.setDataTarget((UUID) target.id, dataRangeStart, Instant.now());
    }

    private void onLineDataUpdate(SampleZone zone, List<SampleViewModel.DataPoint> data) {
        Log.d("ChartFragment", "Got update for single-line data in Zone: "
                + zone.toString() + " " + data.size() + " items in line. (divisor = "
                + timeScaleDivisor + ")");

        // Apply a few transformations:
        //  - Change to representation the chart can consume (DataPoint -> Entry)
        //  - adjust x-axis values to be relative to now, so they show as some amount of time ago
        //      rather than obtuse unix time values.
        Instant now = Instant.now();
        Function<Instant, Long> deltaSeconds = (inputTime) ->
                Duration.between(now, inputTime).getSeconds();

        List<Entry> entries = data.stream()
                .map(dataPoint -> new Entry(deltaSeconds.apply(dataPoint.first), dataPoint.second.floatValue()))
                .collect(Collectors.toList());

        processEntries(zone, entries);
        markReceived(zone);
    }

    /**
     * Converts the given entries to a line data set and stores in the fragment instance.
     * Styling and other configuration are applied to the line as appropriate.
     * @param zone the zone the entries are associated to.
     * @param entries the sequence of entries to process.
     */
    private void processEntries(SampleZone zone, List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, zone.toString());

        dataSet.setColor(colorForZone(zone));
        dataSet.setDrawCircles(false);

        // update cached lines.
        lineDataSets[zone.ordinal()] = dataSet;
    }

    private void markReceived(SampleZone zone) {
        dataPending[zone.ordinal()] = false;
        boolean any = false;
        for (boolean pending : dataPending) {
            if (pending) {
                any = true;
                break;
            }
        }

        if (!any) {
            // all updates received.
            // collect only the 'shown' lines, compose a line data object with them.
            LineData chartData = collectShownLines();

            // update chart.
            commitToChart(chartData);

            // Data processed & sent to chart, remove progress indicator
            requireView().findViewById(R.id.progress_circular).setVisibility(View.GONE);
        }
    }

    private LineData collectShownLines() {
        return new LineData(
                Arrays.stream(SampleZone.values())
                        .filter(this::lineShouldBeShown)
                        .map(z -> lineDataSets[z.ordinal()])
                        .collect(Collectors.toList()));
    }

    private Instant rangeStart(@IdRes int activeRadioId) {
        int unitCount = 0;
        if (activeRadioId == R.id.rb_time_range_15_minute) unitCount = 15;
        else if (activeRadioId == R.id.rb_time_range_30_minute) unitCount = 30;
        else if (activeRadioId == R.id.rb_time_range_hour) unitCount = 60;
        else if (activeRadioId == R.id.rb_time_range_12_hour) unitCount = 60 * 12;
        else if (activeRadioId == R.id.rb_time_range_24_hour) unitCount = 60 * 24;
        else if (activeRadioId == R.id.rb_time_range_7_day) unitCount = 60 * 24 * 7;
        return Instant.now().minus(unitCount, ChronoUnit.MINUTES);
    }

    private boolean lineShouldBeShown(SampleZone zone) {
        CheckBox checkBox;
        switch (zone) {
            case HSIE:
                checkBox = requireView().findViewById(R.id.toggle_hsie);
                break;
            case HSUE:
                checkBox = requireView().findViewById(R.id.toggle_hsue);
                break;
            case MIE:
                checkBox = requireView().findViewById(R.id.toggle_mie);
                break;
            case CSIE:
                checkBox = requireView().findViewById(R.id.toggle_csie);
                break;
            case CSUE:
                checkBox = requireView().findViewById(R.id.toggle_csue);
                break;
            default:
                return false;
        }

        return checkBox != null && checkBox.isChecked();
    }

    @IdRes
    private int toggleButtonForZone(SampleZone zone) {
        switch (zone) {
            case HSIE: return R.id.toggle_hsie;
            case HSUE: return R.id.toggle_hsue;
            case MIE:  return R.id.toggle_mie;
            case CSIE: return R.id.toggle_csie;
            case CSUE: return R.id.toggle_csue;
            default: throw new IllegalArgumentException();
        }
    }

    @ColorInt
    private int colorForZone(SampleZone zone) {
        Function<Integer, Integer> extractor =
                (resId) -> ContextCompat.getColor(requireContext(), resId);

        switch (zone) {
            case HSIE:
                return extractor.apply(R.color.chart_therm_hot_glass);
            case HSUE:
                return extractor.apply(R.color.chart_therm_hot_mat);
            case MIE:
                return extractor.apply(R.color.chart_therm_mid_glass);
            case CSIE:
                return extractor.apply(R.color.chart_therm_cold_glass);
            case CSUE:
                return extractor.apply(R.color.chart_therm_cold_mat);
            default:
                return Color.BLACK;
        }
    }

    /**
     * Applies one-off configuration to the line chart.
     * Assumes the chart view has already been inflated and assigned to `lineChart`.
     */
    private void initChart() {
        if (lineChart == null) return;

        lineChart.fitScreen();
        lineChart.animateX(250);

        lineChart.getXAxis().enableGridDashedLine(1f, 0.5f, 0f);
        lineChart.getAxisLeft().enableGridDashedLine(1f, 0.5f, 0f);

        lineChart.getAxisRight().setEnabled(false);

        lineChart.getAxisRight().setDrawLabels(false); // don't bother with formatting dates (yet)
        lineChart.getAxisLeft().setDrawLabels(true);   // show temperatures

        lineChart.setTouchEnabled(false);

        // While the chart view seems to handle day-night theming alright, it seems that the text
        // (legend, labels, etc) need some help
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.colorOnPrimarySurface, typedValue, true);
        @ColorInt int colorOnSurface = typedValue.data;

        lineChart.getLegend().setTextColor(colorOnSurface);
        lineChart.getAxisLeft().setTextColor(colorOnSurface);
        lineChart.getXAxis().setTextColor(colorOnSurface);
    }

    /**
     * Returns radio buttons & check boxes to a default state.
     * @param baseView the view to find the buttons in. Should not be null if the fragments view
     *                 has not yet been set. (e.g. calling from onCreateView
     *                 can be null otherwise - the fragment's view will be used.
     */
    private void setDefaultState(View baseView) {
        baseView = (baseView == null) ? requireView() : baseView;

        RadioButton defaultButton = radioGroup.findViewById(R.id.rb_time_range_15_minute);
        defaultButton.setChecked(true);
        timeScaleDivisor = getTimeDivisorFor(R.id.rb_time_range_hour);

        for (SampleZone zone : SampleZone.values()) {
            ((CheckBox) baseView.findViewById(toggleButtonForZone(zone))).setChecked(true);
        }
    }

    /**
     * Applies behavior listeners to check boxes & radio buttons.
     * @param baseView the view to find the buttons in. Should not be null if the fragments view
     *                 has not yet been set. (e.g. calling from onCreateView
     *                 can be null otherwise - the fragment's view will be used.
     */
    private void bindButtonActions(View baseView) {
        baseView = (baseView == null) ? requireView() : baseView;

        for (SampleZone zone : SampleZone.values()) {
            CheckBox checkBox = baseView.findViewById(toggleButtonForZone(zone));
            checkBox.setOnCheckedChangeListener((button, isChecked) -> {
                // recollect shown lines & send to chart.
                LineData data = lineChart.getData();
                if (data == null)
                    return; // can't do anything yet - no data has been loaded.

                if (isChecked) {
                    // add the line to chart.
                    data.addDataSet(lineDataSets[zone.ordinal()]);
                }
                else {
                    // WARN: if we're unlucky it is possible that the LineDataSet stored in
                    //       lineDataSets is no longer *in* the chart.
                    //       Here's hoping the user doesn't push a bunch of buttons at once, eh?
                    data.removeDataSet(lineDataSets[zone.ordinal()]);
                }

                lineChart.notifyDataSetChanged();
                lineChart.invalidate();
            });
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // set the new divisor - used when transforming data:
            timeScaleDivisor = getTimeDivisorFor(checkedId);

            /* If we're moving from a broader time range to a narrower one, we can short cut the
             * lengthy data access & refresh and just remove entries that are leaving scope.
             *
             * However, this creates some problems where we may not retrieve new samples (which will
             * be accentuated by the narrower time scale).
            */
            Instant newStartPoint = rangeStart(checkedId);
            notifyModel(currentTarget, newStartPoint);
        });
    }

    /**
     * Sends the LineData object to the line chart, setting some other chart
     * characteristics in the process.
     * @param data data to be set as the chart's data
     */
    private void commitToChart(LineData data) {
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setAxisMaximum(0f);
        xAxis.setAxisMinimum(Duration.between(Instant.now(), dataStart).getSeconds());
        xAxis.setValueFormatter(new ValueFormatter() {
            @SuppressLint("DefaultLocale")
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", Math.abs(value / timeScaleDivisor));
            }
        });

        // Adjust the x axis title:
        TextView xAxisTitle = requireView().findViewById(R.id.chart_x_axis_title);
        String unitString = requireContext().getString(getTimeUnitFor(radioGroup.getCheckedRadioButtonId()));
        xAxisTitle.setText(requireContext().getString(R.string.x_axis_title_format, unitString));

        lineChart.setData(data);
        lineChart.invalidate();
    }

    /**
     * Gets a value with which it is most appropriate to subdivide a seconds value.
     * E.g., if the current date range spans multiple days, then the number of seconds
     * in a day will be returned.
     * @return
     */
    private float getTimeDivisorFor(int radioId) {
        if (radioId == R.id.rb_time_range_hour
                || radioId == R.id.rb_time_range_30_minute
                || radioId == R.id.rb_time_range_15_minute) return 60f;
        else if (radioId == R.id.rb_time_range_12_hour || radioId == R.id.rb_time_range_24_hour)
            return 3600f;
        else if (radioId == R.id.rb_time_range_7_day)
            return 3600f * 24;
        else throw new IllegalStateException("Unsure which date range is selected");
    }

    @StringRes
    private int getTimeUnitFor(int radioId) {
        if (radioId == R.id.rb_time_range_hour
                || radioId == R.id.rb_time_range_30_minute
                || radioId == R.id.rb_time_range_15_minute) return R.string.x_axis_unit_minutes;
        else if (radioId == R.id.rb_time_range_12_hour || radioId == R.id.rb_time_range_24_hour)
            return R.string.x_axis_unit_hours;
        else if (radioId == R.id.rb_time_range_7_day)
            return R.string.x_axis_unit_days;
        else throw new IllegalStateException("Unsure which date range is selected");
    }
}