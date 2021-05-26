package com.mesalu.viv2.android_ui.data;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.http.ApiCompat;
import com.mesalu.viv2.android_ui.data.http.ClientFactory;
import com.mesalu.viv2.android_ui.data.http.IImageClient;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * A repository layer for accessing profile images, may be converted into a generic image
 * repository in the future if more image handling becomes required.
 */
public class ProfileImageRepository {
    protected static ProfileImageRepository instance;
    protected IImageClient imageClient;

    protected ProfileImageRepository() {
        imageClient = ClientFactory.getImageClient();
    }

    public static ProfileImageRepository getInstance() {
        if (instance == null)
            instance = new ProfileImageRepository();
        return instance;
    }

    /**
     * Get the image set as the profile image for the pet indicated by petId.
     * NOTE: the callback is invoked on the main thread regardless of which thread
     *  performs this call
     * @param petId Id of the pet whose image should be acquired
     * @param callback
     */
    public void getPetImage(int petId, Consumer<Drawable> callback) {
        Uri uri = ApiCompat.imageUriFromPet(petId);
        imageClient.acquireImage(uri, callback);
    }

    /**
     * gets the image configured as the pet's profile image, assigning the ImageView's content
     * to the acquired bitmap. Note that this requires a weak-reference be kept to the image view.
     * @param petId id of the pet to acquire an image for.
     * @param into ImageView to assign the acquired image into
     */
    public void getPetImage(int petId, ImageView into) {
        Uri uri = ApiCompat.imageUriFromPet(petId);
        final WeakReference<ImageView> intoRef = new WeakReference<>(into);

        imageClient.acquireImage(uri, drawable -> {
            ImageView view = intoRef.get();
            if (view != null && drawable != null) {
                // imageView is still (plausibly?) valid.
                view.setImageDrawable(drawable);
            }
        });
    }

    /**
     * gets the image configured as the pet's profile image, assigning the ImageView's content
     * to the acquired bitmap. Note that this requires a weak-reference be kept to the image view.
     * @param petId id of the pet to acquire an image for.
     * @param into ImageView to assign the acquired image into
     * @param errorDrawableId the drawable to use should image acquisition fail.
     */
    public void getPetImage(int petId, ImageView into, @DrawableRes int errorDrawableId) {
        Uri uri = ApiCompat.imageUriFromPet(petId);
        final WeakReference<ImageView> intoRef = new WeakReference<>(into);

        imageClient.acquireImage(uri, drawable -> {
            ImageView view = intoRef.get();
            if (view != null) {
                view.setImageDrawable(drawable);
            }
        }, R.drawable.profile_image_placeholder, R.drawable.profile_image_error);
    }
}
