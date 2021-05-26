package com.mesalu.viv2.android_ui.data.http.okhttp.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.mesalu.viv2.android_ui.BuildConfig;
import com.mesalu.viv2.android_ui.ImageUtils;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.http.IImageClient;
import com.mesalu.viv2.android_ui.data.http.okhttp.OkHttpClientFactory;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import java.util.function.Consumer;

public class ImageClient implements IImageClient {
    protected Picasso picasso;
    protected Transformation circularTransform;
    protected Context ctx;

    protected static boolean picassoInitialized;

    public ImageClient(Context context) {
        ctx = context.getApplicationContext();
        initPicasso(this, ctx);
        circularTransform = new Transformation() {
            @Override
            public Bitmap transform(Bitmap source) {
                Bitmap result = ImageUtils.centerCircleCrop(source);
                source.recycle();
                return result;
            }

            @Override
            public String key() {
                return "Picasso-ImageClient-circularTransform";
            }
        };
    }

    /**
     * Initializes and sets picasso's global instance, deferring the retention of a
     * context to picasso (and hopefully mitigating potential reference leaks.)
     * @param instance the constructing instance of ImageClient
     * @param ctx context used to get application context, used to construct picasso builder
     */
    protected static synchronized void initPicasso(ImageClient instance, Context ctx) {
        if (picassoInitialized) {
            instance.picasso = Picasso.get();
        }
        else {
            instance.picasso = new Picasso.Builder(ctx.getApplicationContext())
                    .downloader(new OkHttp3Downloader(OkHttpClientFactory.getInstance().getClient()))
                    .indicatorsEnabled(true)
                    .build();
        }
    }

    @Override
    public boolean cachesResults() {
        return true;
    }

    @Override
    public void acquireImage(Uri uri, @NonNull Consumer<Drawable> callback) {
        picasso.load(uri)
                .transform(circularTransform)
                .into(targetFromConsumer(callback));
    }

    @Override
    public void acquireImage(Uri imageUri, @NonNull Consumer<Drawable> callback, @NonNull Drawable placeHolder, @NonNull Drawable errorDrawable) {
        picasso.load(imageUri)
                .transform(circularTransform)
                .placeholder(placeHolder)
                .error(errorDrawable)
                .into(targetFromConsumer(callback));
    }

    @Override
    public void acquireImage(Uri imageUri, @NonNull Consumer<Drawable> callback, int placeHolder, int errorDrawable) {
        picasso.load(imageUri)
                .transform(circularTransform)
                .placeholder(placeHolder)
                .error(errorDrawable)
                .into(targetFromConsumer(callback));
    }

    protected void postToMainLooper(Consumer<Drawable> callback, Drawable result) {
        Handler handler = new Handler(ctx.getMainLooper());
        handler.post(() -> callback.accept(result));
    }

    protected Target targetFromConsumer(Consumer<Drawable> consumer) {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // invoke `callback` on the main thread:
                // TODO: Since picasso can't handle drawing to a target image view (not given)
                //  it can't place the debug indicators, so if we want similar behavior
                //  we'll have to enforce it via `from`

                Drawable drawable = new BitmapDrawable(ctx.getResources(), bitmap);
                postToMainLooper(consumer, drawable);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                // invoke callback on main thread passing null.
                postToMainLooper(consumer, errorDrawable);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                postToMainLooper(consumer, placeHolderDrawable);
            }
        };
    }
}
