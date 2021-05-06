package com.mesalu.viv2.android_ui.ui.widgets;

import android.content.Context;
import android.graphics.drawable.LevelListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.mesalu.viv2.android_ui.R;

import java.text.NumberFormat;
import java.util.Locale;

public class LedValueView extends ConstraintLayout {
    public enum LedLevel {
        BAD, WARN, GOOD
    }

    private ImageView imageView;
    private TextView textView;

    public LedValueView(@NonNull Context context) {
        super(context);
        _init();
    }

    public LedValueView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        _init();
    }

    public LedValueView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init();
    }

    public void setLedLevel(LedLevel level) {
        LevelListDrawable drawable = (LevelListDrawable) imageView.getBackground();
        drawable.setLevel(level.ordinal());
        Log.d("LVV", "Set level to: " + level);
    }

    public void setText(@StringRes int resId) {
        textView.setText(resId);
    }

    public void setText(float val, int precision) {
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        nf.setMaximumFractionDigits(precision);
        textView.setText(nf.format(val));
    }

    public void setText(double val, int precision) {
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        nf.setMaximumFractionDigits(precision);
        textView.setText(nf.format(val));
    }

    public void setText(float val) {
        setText(val, 2);
    }

    public void setText(double val) {
        setText(val, 2);
    }

    private void  _init() {
        // inflate layout:
        inflate(getContext(), R.layout.widget_led_and_val, this);
        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.text);
    }
}
