package com.mesalu.viv2.android_ui.data.http;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

public interface IImageClient {
    /**
     * Indicates to the repository layer if this implementaiton caches requests automatically
     * @return true if implementation handles caching.
     */
    boolean cachesResults();

    /**
     * Asynchronously acquires & loads the bitmap specified by imageUri.
     * If the Uri does not represent an image, or if acquisition fails then null
     * is passed to the callback.
     * @param imageUri Uri indicating image resource to load
     * @param callback consumer whose accept method is invoked on success. Input of null indicates
     *                 failure to load image.
     */
    void acquireImage(Uri imageUri, @NonNull Consumer<Drawable> callback);

    /**
     * Asynchronously acquires & loads the bitmap specified by imageUri.
     * If the Uri does not represent an image, or if acquisition fails then the errorDrawable
     * is passed to the callback.
     * @param imageUri Uri indicating image to load.
     * @param callback consumer whose accept method is invoked (potentially repeatedly) with drawables
     *                 to use.
     * @param placeHolder a placeholder to use should the acquisition be expected to take meaningful time.
     * @param errorDrawable a drawable that will be fed to callback should an error occur on acquisition.
     */
    void acquireImage(Uri imageUri, @NonNull Consumer<Drawable> callback,
                      @NonNull Drawable placeHolder, @NonNull Drawable errorDrawable);

    /**
     * Asynchronously acquires & loads the bitmap specified by imageUri.
     * If the Uri does not represent an image, or if acquisition fails then the error drawable
     * is passed to the callback.
     * @param imageUri Uri indicating image to load.
     * @param callback consumer whose accept method is invoked (potentially repeatedly) with drawables
     *                 to use.
     * @param placeHolder a placeholder to use should the acquisition be expected to take meaningful time.
     * @param errorDrawable a drawable that will be fed to callback should an error occur on acquisition.
     */
    void acquireImage(Uri imageUri, @NonNull Consumer<Drawable> callback,
                      @DrawableRes int placeHolder, @DrawableRes int errorDrawable);

    // TODO: could supply an overload for sending loaded images to ImageView instances.
    //       not certain I like having coupling to view components this low in the architecture.
}
