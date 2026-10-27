package com.example.savex.model;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class LineChartView extends View {

    private List<Float> values;
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setColor(Color.parseColor("#B6FF00"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        dotPaint.setColor(Color.parseColor("#B6FF00"));
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setValues(List<Float> values) {
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values == null || values.size() < 2) return;

        float w = getWidth(), h = getHeight();
        float padding = 16f;
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        for (float v : values) { min = Math.min(min, v); max = Math.max(max, v); }
        if (max == min) max = min + 1;

        float stepX = (w - 2 * padding) / (values.size() - 1);
        Float prevX = null, prevY = null;

        for (int i = 0; i < values.size(); i++) {
            float x = padding + i * stepX;
            float normalized = (values.get(i) - min) / (max - min);
            float y = h - padding - normalized * (h - 2 * padding);

            if (prevX != null) canvas.drawLine(prevX, prevY, x, y, linePaint);
            canvas.drawCircle(x, y, 5f, dotPaint);
            prevX = x; prevY = y;
        }
    }
}