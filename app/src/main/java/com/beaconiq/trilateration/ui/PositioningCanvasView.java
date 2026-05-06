package com.beaconiq.trilateration.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.beaconiq.trilateration.positioning.phase1.BeaconSample;

import java.util.HashMap;
import java.util.Map;

public class PositioningCanvasView extends View {

    private Map<String, BeaconPos> beaconMap = new HashMap<>();
    private double[] estimatedPosition;

    private final Paint beaconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint positionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint messagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PositioningCanvasView(Context context) {
        super(context);
        init();
    }

    public PositioningCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PositioningCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        beaconPaint.setColor(0xFF00BCD4);
        beaconPaint.setStyle(Paint.Style.FILL);

        positionPaint.setColor(0xFFB61F23);
        positionPaint.setStyle(Paint.Style.FILL);

        ringPaint.setColor(0x44B61F23);
        ringPaint.setStyle(Paint.Style.FILL);

        labelPaint.setColor(0xFF000000);
        labelPaint.setTextSize(24f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        messagePaint.setColor(0xFF9E9E9E);
        messagePaint.setTextSize(32f);
        messagePaint.setTextAlign(Paint.Align.CENTER);

        borderPaint.setColor(0xFFBDBDBD);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
    }

    public void update(Map<String, BeaconSample> beacons, double[] position) {
        this.beaconMap = new HashMap<>();
        for (Map.Entry<String, BeaconSample> e : beacons.entrySet()) {
            BeaconSample b = e.getValue();
            this.beaconMap.put(e.getKey(), new BeaconPos(b.getX(), b.getY(), b.getFilteredDistance()));
        }
        this.estimatedPosition = position;
        invalidate();
    }

    public void updateP2(Map<String, com.beaconiq.trilateration.positioning.phase2.BeaconSample> beacons, double[] position) {
        this.beaconMap = new HashMap<>();
        for (Map.Entry<String, com.beaconiq.trilateration.positioning.phase2.BeaconSample> e : beacons.entrySet()) {
            com.beaconiq.trilateration.positioning.phase2.BeaconSample b = e.getValue();
            this.beaconMap.put(e.getKey(), new BeaconPos(b.getX(), b.getY(), b.getFilteredDistance()));
        }
        this.estimatedPosition = position;
        invalidate();
    }

    public void clear() {
        beaconMap.clear();
        estimatedPosition = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFFF4F4F4);

        int w = getWidth();
        int h = getHeight();
        canvas.drawRect(0, 0, w, h, borderPaint);
        float pad = 48f;

        if (beaconMap.isEmpty()) {
            canvas.drawText("Need 3+ beacons for positioning",
                    w / 2f, h / 2f, messagePaint);
            return;
        }

        int active = 0;
        for (BeaconPos b : beaconMap.values()) {
            if (b.filteredDistance != null) active++;
        }
        if (active < 3) {
            canvas.drawText("Need 3+ beacons (" + active + " active)",
                    w / 2f, h / 2f, messagePaint);
        }

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (BeaconPos b : beaconMap.values()) {
            minX = Math.min(minX, b.x);
            maxX = Math.max(maxX, b.x);
            minY = Math.min(minY, b.y);
            maxY = Math.max(maxY, b.y);
        }
        if (estimatedPosition != null) {
            minX = Math.min(minX, estimatedPosition[0]);
            maxX = Math.max(maxX, estimatedPosition[0]);
            minY = Math.min(minY, estimatedPosition[1]);
            maxY = Math.max(maxY, estimatedPosition[1]);
        }

        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        if (rangeX < 1) { minX -= 5; maxX += 5; rangeX = 10; }
        if (rangeY < 1) { minY -= 5; maxY += 5; rangeY = 10; }
        minX -= rangeX * 0.15;
        maxX += rangeX * 0.15;
        minY -= rangeY * 0.15;
        maxY += rangeY * 0.15;

        double drawW = w - 2 * pad;
        double drawH = h - 2 * pad;
        double scale = Math.min(drawW / (maxX - minX), drawH / (maxY - minY));
        double offX = pad + (drawW - (maxX - minX) * scale) / 2;
        double offY = pad + (drawH - (maxY - minY) * scale) / 2;

        for (Map.Entry<String, BeaconPos> entry : beaconMap.entrySet()) {
            BeaconPos b = entry.getValue();
            float cx = (float) (offX + (b.x - minX) * scale);
            float cy = (float) (offY + (maxY - b.y) * scale);

            canvas.drawCircle(cx, cy, 18f, beaconPaint);

            String label = entry.getKey();
            String[] parts = label.split(":");
            if (parts.length >= 3) label = parts[2];
            canvas.drawText(label, cx, cy - 24f, labelPaint);
        }

        if (estimatedPosition != null && active >= 3) {
            float px = (float) (offX + (estimatedPosition[0] - minX) * scale);
            float py = (float) (offY + (maxY - estimatedPosition[1]) * scale);
            canvas.drawCircle(px, py, 28f, ringPaint);
            canvas.drawCircle(px, py, 12f, positionPaint);
        }
    }

    private static class BeaconPos {
        final double x, y;
        final Double filteredDistance;
        BeaconPos(double x, double y, Double d) { this.x = x; this.y = y; this.filteredDistance = d; }
    }
}
