package com.eyeverify.eyeprintid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

public class TargetRectangle extends RelativeLayout {

    Paint p;
    Paint bottomGradient;
    Shader bottomGradientShader;
    Path targetLinePath;
    float density;

    int leftRightMargin;
    int topBottomMargin;

    int bgColor = Color.argb(100, 255, 255, 255);

    final double LEFT_RIGHT_MARGIN_FAR = 20.0f;
    final double TOP_BOTTOM_MARGIN_FAR = 30.0f;

    final double LEFT_RIGHT_MARGIN_CLOSE = 5.0f;
    final double TOP_BOTTOM_MARGIN_CLOSE = 7.0f;

    int TARGET_COLOR = Color.rgb(100, 100, 100);
    int TARGET_COLOR_SUCCESS = Color.rgb(100, 200, 100);

    int LINE_COLOR = TARGET_COLOR;
    int LINE_COLOR_SUCCESS = Color.GREEN;

    int targetCornerLength;
    float targetStrokeWidth;
    float targetStrokeWidthSuccess;
    boolean targetSuccess;

    View scanLine;
    TranslateAnimation scanLineAnimation;

    int availableHeight;
    int interiorWidth;
    int interiorHeight;

    void init(Context context) {
        this.setWillNotDraw(false);

        p = new Paint();

        TARGET_COLOR = context.getResources().getColor(R.color.target_box_no_eyes);
        LINE_COLOR = TARGET_COLOR;
        TARGET_COLOR_SUCCESS = context.getResources().getColor(R.color.target_box_has_eyes);
        LINE_COLOR_SUCCESS = TARGET_COLOR_SUCCESS;

        density = context.getResources().getDisplayMetrics().density;

        leftRightMargin = (int) (density * LEFT_RIGHT_MARGIN_FAR);
        topBottomMargin = (int) (density * TOP_BOTTOM_MARGIN_FAR);
        availableHeight = (int) (200.0f * density);

        targetCornerLength = (int) (20.0f * density);
        targetStrokeWidth = 3.0f * density;
        targetStrokeWidthSuccess = 5.0f * density;

        scanLine = new View(getContext()) {
            Paint scanlinePaint;

            int lastLineColor = -1;

            @Override
            protected void onDraw(Canvas canvas) {
                //super.onDraw(canvas);

                int lineColor = targetSuccess ? LINE_COLOR_SUCCESS : LINE_COLOR;

                if (scanlinePaint == null || lastLineColor != lineColor) {
                    scanlinePaint = new Paint();
                    scanlinePaint.setStrokeWidth(getHeight());
                    scanlinePaint.setColor(lineColor);
                    lastLineColor = lineColor;
                }

                canvas.drawLine(0, 0, getWidth(), getHeight(), scanlinePaint);
            }
        };

        scanLine.setBackgroundColor(Color.TRANSPARENT);
        scanLine.setVisibility(View.GONE);
        scanLine.setAlpha(0.6f);

        addView(scanLine);
    }

    public TargetRectangle(Context context) {
        super(context);
        init(context);
    }

    public TargetRectangle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TargetRectangle(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void startScanning() {
        configureScanLine();

        if (scanLine.getVisibility() != View.VISIBLE) {
            scanLine.setVisibility(View.VISIBLE);
        }

        if (scanLineAnimation == null) {
            configureScanLine();
        }

        if (!scanLineAnimation.hasStarted()) {
            scanLine.startAnimation(scanLineAnimation);
        }
    }


    public void recreatePaintAndShaders() {
        bottomGradient = null;
        bottomGradientShader = null;
        targetLinePath = null;
        configureScanLine();

        super.invalidate();
    }

    public void setDistanceClose() {
        leftRightMargin = (int) (density * LEFT_RIGHT_MARGIN_CLOSE);
        topBottomMargin = (int) (density * TOP_BOTTOM_MARGIN_CLOSE);

        recreatePaintAndShaders();
    }

    public void setDistanceFar() {
        leftRightMargin = (int) (density * LEFT_RIGHT_MARGIN_FAR);
        topBottomMargin = (int) (density * TOP_BOTTOM_MARGIN_FAR);
        recreatePaintAndShaders();
    }

    public void stopScanning() {
        scanLine.clearAnimation();
        scanLineAnimation = null;
        scanLine.setVisibility(View.GONE);
    }

    private void configureScanLine() {

        int scanLineHeight = (int) (4.0f * density);

        interiorWidth = getWidth() - 2 * leftRightMargin;


        interiorHeight = availableHeight - 2 * topBottomMargin;
        scanLine.setLayoutParams(new LayoutParams(interiorWidth, scanLineHeight));

        scanLineAnimation = new TranslateAnimation(leftRightMargin, leftRightMargin, topBottomMargin, topBottomMargin + interiorHeight - scanLineHeight);
        scanLineAnimation.setDuration(1000);
        scanLineAnimation.setFillAfter(true);
        scanLineAnimation.setRepeatCount(-1);
        scanLineAnimation.setRepeatMode(Animation.REVERSE);

        //scanLine.setAnimation(scanLineAnimation);

        if (scanLine.getVisibility() == View.VISIBLE) {
            scanLine.startAnimation(scanLineAnimation);
        }
    }

    protected void onLayout(boolean changed,
                                     int l, int t, int r, int b) {
        super.onLayout(changed,l, t, r, b);

        if (changed) {
            configureScanLine();
        }
    }

    public void setTargetSuccess(boolean isSuccess) {
        targetSuccess = isSuccess;
        scanLine.invalidate();
        invalidate();
    }

    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        interiorWidth = getWidth() - 2 * leftRightMargin;
        interiorHeight = availableHeight - 2 * topBottomMargin;

        //draw BG border
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL);
        p.setColor(bgColor);

        //White overlay
        canvas.drawRect(0.0f, 0.0f, getWidth(), topBottomMargin, p); //top
        canvas.drawRect(getWidth() - leftRightMargin, topBottomMargin, getWidth(), availableHeight - topBottomMargin, p); //right
        canvas.drawRect(0, topBottomMargin, leftRightMargin, availableHeight - topBottomMargin, p); //left

        //bottom gradient
        int bottomStart = availableHeight - topBottomMargin;
        if (bottomGradient == null) {
            bottomGradient = new Paint();
            bottomGradientShader = new LinearGradient(getWidth()/2, bottomStart, getWidth()/2, getHeight(), bgColor, Color.WHITE, Shader.TileMode.CLAMP);
            bottomGradient.setShader(bottomGradientShader);
        }
        canvas.drawRect(0.0f, bottomStart, getWidth(), getHeight(), bottomGradient);

        //draw target
        p.setAlpha(255);
        p.setStyle(Paint.Style.STROKE);

        p.setStrokeWidth(targetSuccess ? targetStrokeWidthSuccess : targetStrokeWidth);
        p.setColor(targetSuccess ? TARGET_COLOR_SUCCESS : TARGET_COLOR);

        int targetOffset = Math.round((targetStrokeWidth + 1) / 2.0f);

        //clockwise from top left including left side of it:

        if (targetLinePath == null) {
            targetLinePath = new Path();

            //top left corner
            targetLinePath.moveTo(leftRightMargin - targetOffset, topBottomMargin + targetCornerLength - targetOffset);
            targetLinePath.lineTo(leftRightMargin - targetOffset, topBottomMargin - targetOffset);
            targetLinePath.lineTo(leftRightMargin + targetCornerLength - targetOffset, topBottomMargin - targetOffset);

            //top right corner
            targetLinePath.moveTo(leftRightMargin + interiorWidth - targetCornerLength + targetOffset, topBottomMargin - targetOffset);
            targetLinePath.lineTo(leftRightMargin + interiorWidth + targetOffset, topBottomMargin - targetOffset);
            targetLinePath.lineTo(leftRightMargin + interiorWidth + targetOffset, topBottomMargin + targetCornerLength - targetOffset);

            //bottom right corner
            targetLinePath.moveTo(leftRightMargin + interiorWidth + targetOffset, topBottomMargin + interiorHeight - targetCornerLength + targetOffset);
            targetLinePath.lineTo(leftRightMargin + interiorWidth + targetOffset, topBottomMargin + interiorHeight + targetOffset);
            targetLinePath.lineTo(leftRightMargin + interiorWidth - targetCornerLength + targetOffset, topBottomMargin + interiorHeight + targetOffset);

            //bottom left corner
            targetLinePath.moveTo(leftRightMargin + targetCornerLength - targetOffset, topBottomMargin + interiorHeight + targetOffset);
            targetLinePath.lineTo(leftRightMargin - targetOffset, topBottomMargin + interiorHeight + targetOffset);
            targetLinePath.lineTo(leftRightMargin - targetOffset, topBottomMargin + interiorHeight - targetCornerLength + targetOffset);
        }

        canvas.drawPath(targetLinePath, p);
    }

}
