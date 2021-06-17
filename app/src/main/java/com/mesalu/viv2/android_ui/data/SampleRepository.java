package com.mesalu.viv2.android_ui.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.http.IDataAccessClient;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Handles the acquisition, caching, and delivering of EnvDataSamples.
 */
public final class SampleRepository {
    // To reduce type-name length.

    private static final class SamplePage {
        List<EnvDataSample> samples;
        Instant pageStart;
        Instant firstSampleTime;

        public SamplePage(@NonNull List<EnvDataSample> samples, Instant pageStart) {
            this.samples = samples;
            this.pageStart = pageStart;

            // NOTE: ideally the discovered sample should be the first sample of the batch.
            samples.stream()
                    .min(Comparator.comparingLong(sample -> sample.getCaptureTime().toEpochSecond()))
                    .ifPresent(envDataSample -> firstSampleTime = envDataSample.getCaptureTime().toInstant());
        }

        /**
         * @return the samples contained within this page.
         */
        public List<EnvDataSample> getSamples() {
            return samples;
        }

        /**
         * @return the instant for which the page frame starts.
         */
        public Instant getPageStart() {
            return pageStart;
        }

        /**
         * Gets the earliest sample time in the page.
         * This is ensured to be no earlier than the page frame's start instant.
         * @return the instant the first sample within the page occurred
         */
        public Instant getFirstSampleTime() {
            return firstSampleTime;
        }
    }

    private static final class SampleMap extends HashMap<Instant, SamplePage> {}

    /**
     * Represents a request for a time slice.
     * Requested ranges may not fall precisely within page brackets, additionally
     * pages may be received out of order. Both of these require some extra info and to track
     * and post process. The RequestContext class contains this information as well as some
     * methods to help
     */
    private class RequestContext {
        Instant begin, end;
        Instant startPage, endPage;
        SampleMap pageMap;
        HashMap<Instant, Boolean> pendingMap;
        Consumer<List<EnvDataSample>> onCompleteListener;

        RequestContext(int petId, Instant begin, Instant end,
                       @NonNull Consumer<List<EnvDataSample>> onComplete) {
            this.begin = begin;
            this.end = end;
            onCompleteListener = onComplete;

            pageMap = getPetPage(petId);
            pendingMap = new HashMap<>();

            // determine which pages we'll need
            long excess = begin.getEpochSecond() % pageLengthSeconds;
            startPage = Instant.ofEpochSecond(begin.getEpochSecond() - excess);

            excess = end.getEpochSecond() % pageLengthSeconds;
            endPage = Instant.ofEpochSecond(end.getEpochSecond() - excess);

            // run through each page within & add to pending state.
            for (Instant cursor = startPage; cursor.isBefore(endPage) || cursor.equals(endPage); cursor = cursor.plusSeconds(pageLengthSeconds)) {
                boolean havePage = pageMap.containsKey(cursor);
                pendingMap.put(cursor, havePage);
                if (!havePage) {
                    // kick off request sequence here?
                }
            }
        }

        void recvPage(SamplePage page) {
            // add to main cache:
            pageMap.put(page.getPageStart(), page);

            // mark done in pending map
            pendingMap.put(page.getPageStart(), true);

            if (checkForCompletion()) {
                complete();
            }
        }

        /**
         * Checks if all pages are accounted for, if so onCompleteListener is called
         * with a flattened list of results
         */
        boolean checkForCompletion() {
            // if all pending requests are done, then signal for request fulfillment.
            return pendingMap.values().stream().allMatch(x -> x);
        }

        /**
         * invokes the completion callback listener with a flattened version of page data.
         */
        void complete() {
            onCompleteListener.accept(getFlattened());
        }

        List<EnvDataSample> getFlattened() {
            return pageMap
                    .values()
                    .stream()
                    .sorted((a, b) -> a.getPageStart().compareTo(b.getPageStart()))
                    .flatMap(page -> page.getSamples().stream())
                    .filter(sample -> {
                        Instant sampleTime = sample.getCaptureTime().toInstant();
                        return (begin.isBefore(sampleTime) || begin.equals(sampleTime)) && sampleTime.isBefore(end);
                    })
                    .collect(Collectors.toList());
        }
    }

    private static final int pageLengthHours = 2;
    private static final int pageLengthSeconds = (pageLengthHours * 3600);

    private static SampleRepository instance;

    private final HashMap<Integer, SampleMap> petPages;
    private IDataAccessClient dataClient;

    public static synchronized SampleRepository getInstance() {
        if (instance == null) instance = new SampleRepository();
        return instance;
    }

    private SampleRepository() {
        petPages = new HashMap<>();
    }

    /**
     * Acquires samples in the time range specified by [start, end)
     * @param petId the pet whose associated samples are being requested
     * @param start instant in time marking the start of the range
     * @param end instant in time marking the non-inclusive end of the range.
     * @param callback a consumer to be called with the processed result.
     */
    public void getSamples(int petId, Instant start, Instant end,
                      Consumer<Result<List<EnvDataSample>>> callback) {

        final RequestContext rctx = new RequestContext(petId, start, end,
                sampleList -> {
                    Result.Success<List<EnvDataSample>> result = new Result.Success<>(sampleList);
                    callback.accept(result);
                });

        if (rctx.checkForCompletion()) {
            // all required pages were in cache.
            rctx.complete();
        }
        else {
            rctx.pendingMap.forEach((pageStart, havePage) -> {
                if (!havePage) {
                    fetchPage(petId, pageStart, rctx::recvPage);
                }
            });
        }
    }

    /**
     * The first step in a callback sequence for acquiring a page. This method will first
     * check internal cache for the page, if present then the callback will be invoked with the
     * page's content. Otherwise a call to check the local sqlite database will be invoked
     * which may then fall back to requesting the web api for the content.
     * @param petId ID of the pet whose data is being requested.
     * @param pageStart Instant in time that marks the start of the time range.
     * @param callback Instant in time that marks the end of the time range.
     */
    private void fetchPage(int petId, Instant pageStart, Consumer<SamplePage> callback) {
        // TODO: if the specified page includes now (e.g., pageStart.plus(pageLengthHours).isAfter(now)
        //       then always fetch from API (Unless cached version is < 30 seconds old.)

        // Check if we have the page in cache, Note that this shouldn't be called if that
        // were the case, but better safe than sorry, a refactor may change the assumptions validity
        if (petPages.containsKey(petId)) {
            SampleMap sampleMap = petPages.get(petId);
            if (sampleMap != null && sampleMap.containsKey(pageStart))
                callback.accept(sampleMap.get(pageStart));
        }
        else petPages.put(petId, new SampleMap());

        dbHasPage(petId, pageStart, hasPage -> {
            Instant pageEnd = pageStart.plus(pageLengthHours, ChronoUnit.HOURS);
            if (hasPage) {
                // fetch page from DB.
            }
            else {
                // fetch page from API
                getClient().getSamplesInDateRange(petId, pageStart, pageEnd, result -> {
                    if (result instanceof Result.Success && ((Result.Success<List<EnvDataSample>>) result).getData() != null) {
                        // bundle the sample list into a SamplePage container
                        List<EnvDataSample> sampleList = ((Result.Success<List<EnvDataSample>>) result).getData();
                        SamplePage page = new SamplePage(sampleList, pageStart);

                        // TODO: write through to db.

                        callback.accept(page);
                    }
                    else {
                        // TODO: cancel pending requests & notify of failure.
                        Log.e("SampleRepository", "Failed to acquire page");
                    }
                });
            }
        });
    }

    @NonNull
    private synchronized SampleMap getPetPage(int petId) {
        if (!petPages.containsKey(petId)) petPages.put(petId, new SampleMap());

        SampleMap pageContainer = petPages.get(petId);

        // shouldn't be possible, but may as well catch it (keeps linting happy).
        if (pageContainer == null) throw new RuntimeException("Inconsistent page container state");
        return pageContainer;
    }

    private void dbHasPage(int petId, Instant pageStart, Consumer<Boolean> onResult) {
        // TODO: implement when samples are being cached in db.
        onResult.accept(false);
    }

    private IDataAccessClient getClient() {
        if (dataClient == null) dataClient = ClientFactory.getDataClient();
        return dataClient;
    }
}
