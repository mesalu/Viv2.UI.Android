package com.mesalu.viv2.android_ui.ui.charting;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.mesalu.viv2.android_ui.R;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        if (target.targetType == ChartTarget.TargetType.Pet)
            viewModel.setDataTarget((Integer) target.id, dataRangeStart, Instant.now());

        else if (target.targetType == ChartTarget.TargetType.Environment)
            viewModel.setDataTarget((UUID) target.id, dataRangeStart, Instant.now());
    }

    private void onLineDataUpdate(SampleZone zone, List<SampleViewModel.DataPoint> data) {
        Log.d("ChartFragment", "Got update for single-line data in Zone: "
                + zone.toString() + " " + data.size() + " items in line");

        // Apply a few transformations:
        //  - Change to representation the chart can consume (DataPoint -> Entry)
        //  - adjust x-axis values to be relative to now, so they show as some amount of time ago
        //      rather than obtuse unix time values.
        Instant now = Instant.now();
        Function<Instant, Long> xMapper = (inputTime) ->
            Duration.between(now, inputTime).getSeconds();

        List<Entry> entries = data.stream()
                .map(dataPoint -> new Entry(xMapper.apply(dataPoint.first), dataPoint.second.floatValue()))
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

        if (activeRadioId == R.id.date_range_hour) unitCount = 1;
        else if (activeRadioId == R.id.date_range_12hour) unitCount = 12;
        else if (activeRadioId == R.id.date_range_day) unitCount = 24;
        else if (activeRadioId == R.id.date_range_week) unitCount = 24 * 7;
        else if (activeRadioId == R.id.date_range_month) unitCount = 24 * 30;
        return Instant.now().minus(unitCount, ChronoUnit.HOURS);
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

        RadioButton defaultButton = radioGroup.findViewById(R.id.date_range_hour);
        defaultButton.setChecked(true);

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
            // If we're moving from a broader time range to a narrower one, we can short cut the
            // lengthy data access & refresh and just remove entries that are leaving scope
            Instant newStartPoint = rangeStart(checkedId);
            if (newStartPoint.isAfter(dataStart)) {
                long newSpan = Duration.between(Instant.now(), newStartPoint).getSeconds();

                for (SampleZone zone: SampleZone.values()) {
                    int ord = zone.ordinal();
                    if (lineDataSets[ord] == null) continue;
                    // TODO: throw this into a background task - it could be expensive.
                    processEntries(zone,
                            lineDataSets[ord].getValues()
                                    .stream()
                                    .filter(entry -> entry.getX() > newSpan)
                                    .collect(Collectors.toList()));
                }
                dataStart = newStartPoint; // need to set here since we're not following the typical path
                commitToChart(collectShownLines());
                lineChart.notifyDataSetChanged();
            }
            else {
                // We could cache the different levels if we wanted, to avoid re-querying, but then
                // we'd have even more memory overhead & would have to deal with cache invalidation
                // in an ephemeral component. So I think it's fine to just re-issue a request to the
                // underlying components.
                notifyModel(currentTarget, newStartPoint);
            }
        });
    }

    /**
     * Sends the LineData object to the line chart, setting some other chart
     * characteristics in the process.
     * @param data data to be set as the chart's data
     */
    private void commitToChart(LineData data) {
        lineChart.getXAxis().setAxisMaximum(0f);
        long minimum = Duration.between(Instant.now(), dataStart).getSeconds();
        Log.d("ChartFragment", "Adjusting x min to " + minimum);
        lineChart.getXAxis().setAxisMinimum(minimum);
        lineChart.setData(data);
        lineChart.invalidate();
    }
}