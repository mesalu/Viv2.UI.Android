package com.mesalu.viv2.android_ui.data.http.retrofit;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles retry-logic on requests. Permits configuring when a response should be considered
 * invalid and the request retried.
 *
 * Requests are processed asynchronously, calling the retrier's callback instance only when
 * final-state is achieved (a valid call || failure after all retries are attempted).
 * the .cancel method may be called to stop the retry-chain.
 *
 * Currently no 'backoff' abilities are supported. Though I would like to add them. (Will require
 * a bit of work in getting an executor set up such that final callbacks are processed on the
 * main UI thread. Not sure how to do that yet)
 *
 * Note: I hit a bit of a separation-of-concerns wall here, I wanted a separate builder
 *      class to construct a retrier instance, but generic types and unchecked conversions were
 *      posing an issue. Rather than rely on Retrier's generic member matching the builder's
 *      generic, I decided to merge the two functionalities.
 *
 * @param <TCallModel> The model or model-container to be used for the retrofit Call and Callback
 *                    instances.
 */
public class Retrier <TCallModel> {

    interface RetryPredicate<TCallModel> {
        // Return false if a retry should be performed, true if response is valid.
        boolean responseOk(Call<TCallModel> call, Response<TCallModel> response);
    }

    private int maxTries;
    private int currentTries;
    private Call<TCallModel> call;
    private Call<TCallModel> currentCall;
    private Callback<TCallModel> clientCallback;
    private Callback<TCallModel> callback;
    private RetryPredicate<TCallModel> retryPredicate;
    private boolean started;

    public Retrier() {
        maxTries = 3; // default retry value.
        retryPredicate = (call, response) -> true;
        currentTries = 0;
        started = false;
    }

    public void cancel() {
        this.currentCall.cancel();
    }

    public Retrier<TCallModel> withPredicate(RetryPredicate<TCallModel> predicate) {
        if (started) return this; // don't permit tampering once started.

        retryPredicate = predicate;
        return this;
    }

    public Retrier<TCallModel> withTryCount(int tryCount) {
        if (started) return this; // don't permit tampering once started.

        maxTries = tryCount;
        return this;
    }

    public Retrier<TCallModel> withCall(Call<TCallModel> call) {
        if (started) return this; // don't permit tampering once started.

        this.call = call;
        currentCall = call;
        return this;
    }

    public Retrier<TCallModel> withCallback(Callback<TCallModel> callback) {
        if (started) return this; // don't permit tampering once started.

        this.clientCallback = callback;
        return this;
    }

    /**
     * Enqueues the call, retrying on failure or if `retryPredicate` returns false - up until
     * maxTries has been exceeded, at which point the callback's onFailure method will be called.
     */
    public void proceed() {
        // confirm we got all the required fields:
        if (call == null || clientCallback == null)
            throw new IllegalArgumentException("Retrier not prepared to proceed");

        if (callback == null)
            callback = composeIntermediateCallback();

        started = true;

        enqueueCall(call);
    }

    private void enqueueCall(Call<TCallModel> call) {
        currentTries++;
        currentCall = call;
        call.enqueue(callback);
        Log.d("Retrier", "Queued call for " +
                call.request().url() + " (#" + (currentTries) + "/" + (maxTries) + ")");
    }

    private Callback<TCallModel> composeIntermediateCallback() {
        // Assume all other members are setup:
        return new Callback<TCallModel>() {
            @Override
            public void onResponse(Call<TCallModel> call, Response<TCallModel> response) {
                // check the retry predicate
                if (!retryPredicate.responseOk(call, response)) {
                    if (currentTries <= maxTries)
                        enqueueCall(call.clone());
                    else
                        clientCallback.onFailure(call, new Exception("Predicate failed on retry cap"));
                    return;
                }
                // got a good response & no predicate failure
                clientCallback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<TCallModel> call, Throwable t) {
                if (currentTries <= maxTries)
                    enqueueCall(call.clone());
                else
                    clientCallback.onFailure(call, t);
            }
        };
    }
}
