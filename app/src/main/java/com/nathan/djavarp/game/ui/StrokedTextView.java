package com.nathan.djavarp.game.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.SpannableString;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class StrokedTextView extends AppCompatTextView {

    public StrokedTextView(Context context) {
        super(context);
    }

    public StrokedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StrokedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        CharSequence spannableString = getText();

        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(4f);

        setText(spannableString.toString());
        setTextColor(Color.BLACK);
        super.onDraw(canvas);

        getPaint().setStyle(Paint.Style.FILL);

        setText(spannableString);
        super.onDraw(canvas);
    }
}
