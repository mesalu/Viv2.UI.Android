package com.mesalu.viv2.android_ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * A collection of images for consistently modifying images across supporting image handling
 * implementations
 */
public final class ImageUtils {

    /**
     * Crops the center circle of the given image.
     * Operates in calling thread - as such should be invoked asynchronously from main thread
     * Input image is *not* modified or recycled. Clean up is required of caller.
     */
    public static Bitmap centerCircleCrop(Bitmap source) {
        float radius = Math.min(source.getHeight(), source.getWidth()) / 2f;
        Path circularPath = new Path();
        circularPath.addCircle(source.getWidth() / 2f,
                source.getHeight() / 2f,
                radius, Path.Direction.CCW);

        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.drawPath(circularPath, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, 0, 0, paint);

        return result;
    }
}
