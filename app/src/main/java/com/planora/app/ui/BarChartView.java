package com.planora.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.color.MaterialColors;

/** Small bar chart used for the progress chart in Statistics. */
public class BarChartView extends View {
    private String[] labels = new String[0];
    private int[] values = new int[0];
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context c, AttributeSet a) {
        super(c, a);
        bar.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, 0xFF5B5BD6));
        text.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFF888888));
        text.setTextSize(getResources().getDisplayMetrics().scaledDensity * 11);
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(String[] l, int[] v) {
        labels = l;
        values = v;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = values.length;
        if (n == 0) return;
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        float w = getWidth(), h = getHeight(), labelH = text.getTextSize() * 1.6f;
        float slot = w / n, bw = slot * 0.5f, chartH = h - labelH - text.getTextSize() * 1.4f;
        for (int i = 0; i < n; i++) {
            float cx = slot * i + slot / 2f;
            float bh = chartH * values[i] / max;
            float top = h - labelH - bh;
            c.drawRoundRect(new RectF(cx - bw / 2, top, cx + bw / 2, h - labelH), 10, 10, bar);
            c.drawText(String.valueOf(values[i]), cx, top - 8, text);
            c.drawText(labels[i], cx, h - 6, text);
        }
    }
}
