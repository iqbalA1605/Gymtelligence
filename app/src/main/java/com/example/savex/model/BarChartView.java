package com.example.savex.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class BarChartView extends View {

    private List<Float> values;
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(Color.parseColor("#B6FF00"));
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setValues(List<Float> values) {
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values == null || values.isEmpty()) return;

        float w = getWidth(), h = getHeight();
        float padding = 12f;
        float max = Float.MIN_VALUE;
        for (float v : values) max = Math.max(max, v);
        if (max == 0) max = 1;

        int count = values.size();
        float slotWidth = (w - 2 * padding) / count;
        float barWidth = slotWidth * 0.5f;

        for (int i = 0; i < count; i++) {
            float barHeight = (values.get(i) / max) * (h - 2 * padding);
            float left = padding + i * slotWidth + (slotWidth - barWidth) / 2;
            float top = h - padding - barHeight;
            float right = left + barWidth;
            float bottom = h - padding;
            canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, barPaint);
        }
    }
}