package com.nelson.circlelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;


/**
 * Created by Nelson on 2017/4/18.
 *
 */

public class CircleLayout extends ViewGroup implements GestureDetector.OnGestureListener {

    private final int mTouchSlop;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;
    private double mMinimumCornerVelocity;

    private int mRadius = 250;//子item的中心和整个layout中心的距离
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;
    private GestureDetectorCompat mDetector;

    private int mCenterX;
    private int mCenterY;
    private double mChangeCorner = 0.0;
    private Pair<Float, Float> mStart;
    private boolean isCanScroll = false;
    private boolean isDragging = false;
    private FlingRunnable mFlingRunnable;

    private View mCenterView;

    private float lastX;
    private float lastY;
    private boolean isFling;

    private Pair<Float, Float> beforeFling;


    public CircleLayout(Context context) {
        this(context, null);
    }

    public CircleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDetector = new GestureDetectorCompat(context, this);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledPagingTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CircleLayout, defStyleAttr, defStyleAttr);
        if (attrs != null) {
            try {
                mRadius = (int) a.getDimension(R.styleable.CircleLayout_radium, 250);
            } finally {
                a.recycle();
            }

        }


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();//item的数量

        //可用宽高
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int ps = getPaddingStart();
        int pe = getPaddingEnd();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();

        setMeasuredDimension(widthSize, heightSize);

        //子view最高多少，最宽多少
        int childMaxWidth = 0;
        int childMaxHeight = 0;
        View child;
        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            child.measure(MeasureSpec.makeMeasureSpec(widthSize - ps - pe, MeasureSpec.UNSPECIFIED)
                    , MeasureSpec.makeMeasureSpec(heightSize - pt - pb, MeasureSpec.UNSPECIFIED));

            childMaxWidth = Math.max(childMaxWidth, child.getMeasuredWidth());
            childMaxHeight = Math.max(childMaxHeight, child.getMeasuredHeight());
        }

        int width = resolveAdjustedSize(mRadius * 2 + childMaxWidth + ps + pe, mMaxWidth, widthMeasureSpec);
        int height = resolveAdjustedSize(mRadius * 2 + childMaxHeight + pt + pb, mMaxHeight, heightMeasureSpec);

        int finalWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec));
        int finalHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec));
        setMeasuredDimension(finalWidthSpec, finalHeightSpec);

    }

    private int resolveAdjustedSize(int desiredSize, int maxSize,
                                    int measureSpec) {
        int result = desiredSize;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                /* Parent says we can be as big as we want. Just don't be larger
                   than max size imposed on ourselves.
                */
                result = Math.min(desiredSize, maxSize);
                break;
            case MeasureSpec.AT_MOST:
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                result = Math.min(Math.min(desiredSize, specSize), maxSize);
                break;
            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int childCount = mCenterView == null ? getChildCount() : getChildCount() - 1;//item的数量
        mCenterX = (getMeasuredWidth() - getPaddingStart() - getPaddingEnd()) / 2;
        mCenterY = (getMeasuredHeight() - getPaddingBottom() - getPaddingTop()) / 2;

        View child;
        int childWidth;//item的宽
        int childHeight;//item的高
        double corner;//旋转角度

        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }
            corner = 360 / childCount * i;

            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();


            int cX = (int) (mCenterX - mRadius * Math.cos(Math.toRadians(corner + mChangeCorner)));
            int cY = (int) (mCenterY - mRadius * Math.sin(Math.toRadians(corner + mChangeCorner)));

            child.layout(cX - childWidth / 2, cY - childHeight / 2, cX + childWidth / 2, cY + childHeight / 2);

        }
        if (mCenterView != null) {
            mCenterView.layout(mCenterX - mCenterView.getMeasuredWidth() / 2, mCenterY - mCenterView.getMeasuredHeight() / 2
                    , mCenterX + mCenterView.getMeasuredWidth() / 2, mCenterY + mCenterView.getMeasuredHeight() / 2);
        }

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            return false;
        }

        switch (action) {

            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();

                mStart = null;
                if (mFlingRunnable != null) {
                    mFlingRunnable.endFling();
                }
                if (isFling) {
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float x = ev.getX();
                float y = ev.getY();
                isDragging = Math.sqrt(Math.pow((x - lastX), 2) + Math.pow((y - lastY), 2)) > mTouchSlop;
                return isDragging;


        }
        return super.onInterceptTouchEvent(ev);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return (isCanScroll || isFling) && mDetector.onTouchEvent(event);
    }

    @SuppressWarnings("unused")
    public int getRadius() {
        return mRadius;
    }

    @SuppressWarnings("unused")
    public void setRadius(int mRadius) {
        this.mRadius = mRadius;
        requestLayout();
    }

    @SuppressWarnings("unused")
    public int getMaxWidth() {
        return mMaxWidth;
    }

    @SuppressWarnings("unused")
    public void setMaxWidth(int mMaxWidth) {
        this.mMaxWidth = mMaxWidth;
    }

    @SuppressWarnings("unused")
    public int getMaxHeight() {
        return mMaxHeight;
    }

    @SuppressWarnings("unused")
    public void setMaxHeight(int mMaxHeight) {
        this.mMaxHeight = mMaxHeight;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        if (mStart == null) {
            mStart = new Pair<>(e2.getX() - mCenterX, e2.getY() - mCenterY);
        }

        Pair<Float, Float> end = new Pair<>(e2.getX() - mCenterX, e2.getY() - mCenterY);//结束向量
        //角度
        Double changeCorner = Math.toDegrees(Math.acos((mStart.first * end.first + mStart.second * end.second) / (Math.sqrt(mStart.first * mStart.first +
                mStart.second * mStart.second) * Math.sqrt(end.first * end.first + end.second * end.second))));

        //方向 >0 为顺时针 <0 为逆时针
        double changeDirection = mStart.first * end.second - mStart.second * end.first;

        if (!changeCorner.isNaN()) {
            if (changeDirection > 0) {
                mChangeCorner = (mChangeCorner + changeCorner) % 360;
            } else if (changeDirection < 0) {
                mChangeCorner = (mChangeCorner - changeCorner) % 360;
            }
        }

        requestLayout();
        beforeFling = new Pair<>(mStart.first, mStart.second);
        mStart = end;
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null) {
            return false;
        }
        isFling = true;

        //合速度
        double v = Math.min(Math.sqrt(Math.pow(velocityX, 2) + Math.pow(velocityY, 2)), mMaximumVelocity);

        //三边长
        double oe1 = Math.sqrt(Math.pow(beforeFling.first, 2) + Math.pow(beforeFling.second, 2));
        double oe2 = Math.sqrt(Math.pow(e2.getX() - mCenterX, 2) + Math.pow(e2.getY() - mCenterY, 2));
        double e1e2 = Math.sqrt(Math.pow(e2.getX() - e1.getX(), 2) + Math.pow(e2.getY() - e1.getY(), 2));

        double sin = Math.sqrt(Math.pow(1 - (Math.pow(oe2, 2) + Math.pow(e1e2, 2) - Math.pow(oe1, 2)) / (2 * oe2 * e1e2), 2));
        //角速度
        double vc = 180 * v * sin / (Math.PI * oe2);
        //最小角速度
        mMinimumCornerVelocity = 180 * mMinimumVelocity * sin / (Math.PI * oe2);

        Pair<Float, Float> end = new Pair<>(e2.getX() - mCenterX, e2.getY() - mCenterY);//结束向量

        //方向 >0 为顺时针 <0 为逆时针
        double flingDirection = beforeFling.first * end.second - beforeFling.second * end.first;

        if (mFlingRunnable != null) {
            removeCallbacks(mFlingRunnable);
        }

        post(mFlingRunnable = new FlingRunnable(flingDirection > 0 ? vc : -vc));
        return true;
    }

    private class FlingRunnable implements Runnable {
        double v;//初始速度

        FlingRunnable(double v) {
            this.v = v;
        }

        @Override
        public void run() {
            if (Math.abs(v) >= mMinimumCornerVelocity) {
                // Keep the fling alive a little longer
                v /= 1.0666F;
                mChangeCorner = (mChangeCorner + v / 1000 * 16) % 360;
                postDelayed(this, 16);
                requestLayout();
            } else {
                endFling();
            }

        }

        private void endFling() {
            isFling = false;
            removeCallbacks(this);
        }
    }

    @SuppressWarnings("unused")
    public boolean isCanScroll() {
        return isCanScroll;
    }

    /**
     * @param canScroll 设置石否可以旋转
     */
    public void setCanScroll(boolean canScroll) {
        isCanScroll = canScroll;
    }

    @SuppressWarnings("unused")
    public boolean isDragging() {
        return isDragging;
    }

    public void setCenterView(@NonNull View view) {
        if (mCenterView == null) {
            mCenterView = view;
            addView(mCenterView);
        }
        requestLayout();
    }

    public void removeCenterView() {
        if (mCenterView != null) {
            removeView(mCenterView);
            mCenterView = null;
        }
    }

    /**
     * @return 获取中心的view，没有的话就返回null
     */
    public View getmCenterView(){
        return mCenterView;
    }
}
