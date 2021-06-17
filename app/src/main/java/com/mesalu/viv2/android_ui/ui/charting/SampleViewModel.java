package com.mesalu.viv2.android_ui.ui.charting;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mesalu.viv2.android_ui.data.Result;
import com.mesalu.viv2.android_ui.data.SampleRepository;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.ui.BaseViewModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Stores sample sequences retrieved by SampleRepository that are in use by
 * view components.
 */
public class SampleViewModel extends BaseViewModel {

    public static final class DataPoint extends Pair<Instant, Double> {
        public DataPoint(Instant instant, double val) {
            super(instant, val);
        }
    }

    private final MutableLiveData<List<EnvDataSample>> samples;
    private final HashMap<SampleZone, MutableLiveData<List<DataPoint>>> subSequences;

    // I'm still not sure I'm keen on executors being above the repository layer, but needs must.
    private ExecutorService processingExecutor;

    public SampleViewModel() {
        samples = new MutableLiveData<>();

        subSequences = new HashMap<>();
        subSequences.put(SampleZone.HSIE, new MutableLiveData<>());
        subSequences.put(SampleZone.HSUE, new MutableLiveData<>());
        subSequences.put(SampleZone.MIE, new MutableLiveData<>());
        subSequences.put(SampleZone.CSIE, new MutableLiveData<>());
        subSequences.put(SampleZone.CSUE, new MutableLiveData<>());

        startBackgroundProcessing();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopBackgroundProcessing();
    }

    public LiveData<List<EnvDataSample>> getObservable() {
        return samples;
    }

    /**
     * Gets an observable live data that contains, or will contain, an immutable list of samples
     * from the specified zone in the context of the most recent call to 'setDataTarget'
     * @param zone the zone to select data from.
     * @return an observable for recieving updates to the processed data.
     */
    public LiveData<List<DataPoint>> getLineData(SampleZone zone) {
        return subSequences.get(zone);
    }

    /**
     * Sets the data range & target the view model should contain. This override will select samples
     * for a particular pet.
     * @param petId The pet whose sample data should be accessed
     * @param start Beginning of the time range for which samples should be selected
     * @param end Ending of the time range for which samples should be selected.
     */
    public void setDataTarget(int petId, Instant start, Instant end) {
        SampleRepository.getInstance().getSamples(petId, start, end, result -> {
            if (result instanceof Result.Success) {
                List<EnvDataSample> data = ((Result.Success<List<EnvDataSample>>) result).getData();
                samples.setValue(data);

                submitRunnableToExecutor(() -> processSubSequences(data));
            }
            else {
                Log.e("SampleViewModel", "Encountered an error in data request!");
            }
        });
    }

    /**
     * sets the data range & target the view model should contain. This override will
     * select samples for a particular environment.
     * @param envId The environment for which samples should be accessed.
     * @param start Beginning of the time range for which samples should be selected
     * @param end Ending of the time range for which samples should be selected.
     */
    public void setDataTarget(UUID envId, Instant start, Instant end) {
        // Not yet implemented.
    }

    /**
     * Separates the zone data points into their own sequences, storing in the appropriate
     * live data instances. The separated line data is more likely to be consumable
     * by the view instance than the grouped form.
     */
    private void processSubSequences(List<EnvDataSample> samples) {
        // We want to do this in a background thread & post the result back to the main thread.

        // set up individual containers for each zone
        HashMap<SampleZone, List<DataPoint>> containers = new HashMap<>();
        for (SampleZone zone : SampleZone.values()) {
            containers.put(zone, new ArrayList<>(samples.size()));
        }

        // iterate & separate. (O(n * |SampleZone|)), a lot of costly computation that could be saved.
        for (EnvDataSample sample : samples) {
            for (SampleZone zone : SampleZone.values()) {
                double value = getExtractorForZone(zone).apply(sample);
                Objects.requireNonNull(containers.get(zone))
                        .add(new DataPoint(sample.getCaptureTime().toInstant(), value));
            }
        }

        for (SampleZone zone : SampleZone.values()) {
            Objects.requireNonNull(subSequences.get(zone)).postValue(containers.get(zone));
        }
    }

    private void startBackgroundProcessing() {
        if (processingExecutor == null || processingExecutor.isShutdown())
            processingExecutor = Executors.newFixedThreadPool(4);
    }

    private void stopBackgroundProcessing() {
        if (processingExecutor != null && !processingExecutor.isShutdown()) {
            processingExecutor.shutdown();
        }
    }

    private void submitRunnableToExecutor(@NonNull Runnable runnable) {
        if (processingExecutor != null && !processingExecutor.isShutdown())
            processingExecutor.submit(runnable);
    }

    private Function<EnvDataSample, Double> getExtractorForZone(SampleZone zone) {
        switch (zone) {
            case HSIE:
                return EnvDataSample::getHotGlass;
            case HSUE:
                return EnvDataSample::getHotMat;
            case MIE:
                return EnvDataSample::getMidGlass;
            case CSIE:
                return EnvDataSample::getColdGlass;
            case CSUE:
                return EnvDataSample::getColdMat;
            default:
                throw new IllegalArgumentException("Unrecognized zone");
        }
    }
}
