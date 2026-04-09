package com.yourapp.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * A simple overlay view that draws a grid to help align and measure UI elements.
 */
public class DebugGridView extends View {
    private final Paint linePaint = new Paint();
    private final Paint textPaint = new Paint();
    private static final int GRID_DP = 16; // 16dp grid

    public DebugGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setColor(Color.parseColor("#40FF0000")); // Semi-transparent red
        linePaint.setStrokeWidth(1f);
        
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
        
        // Ensure it doesn't catch touch events
        setClickable(false);
        setFocusable(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float density = getResources().getDisplayMetrics().density;
        float spacing = GRID_DP * density;

        int width = getWidth();
        int height = getHeight();

        // Draw vertical lines
        for (float x = 0; x < width; x += spacing) {
            canvas.drawLine(x, 0, x, height, linePaint);
        }

        // Draw horizontal lines
        for (float y = 0; y < height; y += spacing) {
            canvas.drawLine(0, y, width, y, linePaint);
        }
        
        // Draw some labels for reference (optional)
        canvas.drawText("Grid: " + GRID_DP + "dp", 20, height - 20, textPaint);
    }
}
