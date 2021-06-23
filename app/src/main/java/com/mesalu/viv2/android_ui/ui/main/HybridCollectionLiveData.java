package com.mesalu.viv2.android_ui.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Some UI data consumers want elements-as-a-list, others want just sub-elements.
 * Either way, managing a map & list looks about the same either way.
 */
public class HybridCollectionLiveData<TKey extends Comparable<TKey>, TModel> {
    private final MutableLiveData<List<TModel>> listObservable;
    private final Map<TKey, MutableLiveData<TModel>> mapOfObservables;
    private final Function<TModel, TKey> keyFunction;

    public HybridCollectionLiveData(@NonNull Function<TModel, TKey> keyExtractor) {
        keyFunction = keyExtractor;

        mapOfObservables = new HashMap<>();
        listObservable = new MutableLiveData<>();
    }

    public void clear() {
        mapOfObservables.clear();
        listObservable.setValue(new ArrayList<>());
    }

    public LiveData<List<TModel>> getListObservable() {
        return listObservable;
    }

    public LiveData<TModel> getObservableForId(TKey id) {
        return safeGetObservable(id);
    }

    /**
     * Adds modelInstance to the collection, updating both its direct observable
     * as well as the list observable.
     * @param modelInstance
     */
    public void update(@NonNull TModel modelInstance) {
        TKey id = keyFunction.apply(modelInstance);

        safeGetObservable(id).setValue(modelInstance);
        notifyListObservable();
    }

    /**
     * Updates all model instances in the list of models, but only updates the list observable
     * once at the completion of all other udpates.
     * @param models a list of model instances to udpate.
     */
    public void batchUpdate(@NonNull List<TModel> models) {
        // roll through each, then do a list update at the end.
        models.forEach(model -> {
            TKey id = keyFunction.apply(model);
            safeGetObservable(id).setValue(model);
        });
        notifyListObservable();
    }

    private void notifyListObservable() {
        listObservable.setValue(mapOfObservables.values().stream()
                .map(LiveData::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    private MutableLiveData<TModel> safeGetObservable(TKey id) {
        if (!mapOfObservables.containsKey(id)) mapOfObservables.put(id, new MutableLiveData<>());
        return mapOfObservables.get(id);
    }
}
