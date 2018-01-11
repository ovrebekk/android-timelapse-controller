package com.nordicsemi.TimelapseController.gui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.nordicsemi.TimelapseController.R;

/**
 * Created by too1 on 19-Dec-17.
 */

public class DirectionController extends View {
    private boolean mEnabled;

    // Data variables
    private float mSpeedForward = 0.0f;
    private float mSpeedRight = 0.0f;

    // View variables
    private float mSelectorSize, mSelectorRadiusLimit;
    private Drawable mBackgroundDrawable, mSelectorDrawable;
    private RectF mRegion, mRegionPadded, mSelectorRegion;

    private OnDirectionChangedListener mDirectionChangedListener = null;

    public DirectionController(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DirectionController, 0, 0);

        try{
            mEnabled             = a.getBoolean(R.styleable.DirectionController_enabled, true);
            mBackgroundDrawable  = a.getDrawable(R.styleable.DirectionController_background);
            mSelectorDrawable    = a.getDrawable(R.styleable.DirectionController_selectorDrawable);
            mSelectorSize        = a.getDimension(R.styleable.DirectionController_selectorSize, 100.0f);
            mSelectorRadiusLimit = a.getFloat(R.styleable.DirectionController_selectorMoveLimitRadiusFactor, 1.0f);

        }finally {
            a.recycle();
        }
        init();
    }

    private void init(){
        mRegion = new RectF();
        mRegionPadded = new RectF();
        mSelectorRegion = new RectF();
    }

    public void setEnabled(boolean enabled){
        mEnabled = enabled;
        invalidate();
    }

    public void setOnDirectionChangedListener(OnDirectionChangedListener listener){
        mDirectionChangedListener = listener;
    }

    public float getSpeedForward(){
        return mSpeedForward / mSelectorRadiusLimit;
    }

    public float getSpeedRight(){
        return mSpeedRight / mSelectorRadiusLimit;
    }

    private void setSelectorBounds(){
        if(mSelectorDrawable != null) {
            float left = mRegionPadded.left + mRegionPadded.width() * (mSpeedRight + 1.0f) * 0.5f - mSelectorSize * 0.5f;
            float right = left + mSelectorSize;
            float top = mRegionPadded.top + mRegionPadded.height() * (1.0f - mSpeedForward) * 0.5f - mSelectorSize * 0.5f;
            float bottom = top + mSelectorSize;
            mSelectorDrawable.setBounds((int) left, (int) top, (int) right, (int) bottom);
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());
        mRegion.set(getLeft(), getTop(), (float) w, (float) h);
        mRegionPadded.set(getPaddingLeft(), getPaddingTop(), (float) w - getPaddingRight(), (float) h - getPaddingBottom());
        if(mBackgroundDrawable != null) {
            mBackgroundDrawable.setBounds((int) mRegionPadded.left, (int) mRegionPadded.top, (int) mRegionPadded.right, (int) mRegionPadded.bottom);
        }
        setSelectorBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mBackgroundDrawable != null) {
            mBackgroundDrawable.draw(canvas);
        }
        if(mEnabled){
            if(mSelectorDrawable != null) {
                mSelectorDrawable.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float pressAngle, pressRadius;
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE){
            float normalizedX, normalizedY;
            float rawX = event.getX();
            float rawY = event.getY();
            if(mEnabled) {
                if (rawX > mRegionPadded.left && rawX < mRegionPadded.right && rawY > mRegionPadded.top && rawY < mRegionPadded.bottom) {
                    normalizedX = (rawX - mRegionPadded.left) / mRegionPadded.width();
                    normalizedY = (rawY - mRegionPadded.top) / mRegionPadded.height();
                    normalizedX = (normalizedX - 0.5f) * 2.0f;
                    normalizedY = (normalizedY - 0.5f) * 2.0f;

                    pressAngle = (float)Math.atan2((double)normalizedY, (double)normalizedX);
                    //pressAngle = (pressAngle + (float)Math.PI) / (2.0f * (float)Math.PI);
                    pressRadius = (float)Math.sqrt(Math.pow((double) normalizedX, 2.0) + Math.pow((double) normalizedY, 2.0));
                    pressRadius *= (1 / 0.89f);
                    //Log.d("DirectionController", "Angle " + String.valueOf(pressAngle) + ", Radius " + String.valueOf(pressRadius));

                    if(pressRadius > mSelectorRadiusLimit) pressRadius = mSelectorRadiusLimit;
                    if(pressRadius < 0.1f) pressRadius = 0.0f;

                    mSpeedForward = pressRadius * (float)Math.sin(pressAngle) * -1.0f;
                    mSpeedRight = pressRadius * (float)Math.cos(pressAngle);

                    //mSpeedForward = 1.0f - normalizedY * 2.0f;
                    //mSpeedRight = normalizedX * 2.0f - 1.0f;
                    setSelectorBounds();
                    if(mDirectionChangedListener != null){
                        mDirectionChangedListener.onDirectionChanged(this, mSpeedForward / mSelectorRadiusLimit, mSpeedRight / mSelectorRadiusLimit);
                    }
                }
                return true;
            }
        }

        return false;
    }

    public interface OnDirectionChangedListener {
        void onDirectionChanged(DirectionController sender, float speedForward, float speedRight);
    }
}
