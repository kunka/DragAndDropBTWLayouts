package com.kk.control;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xj on 13-7-19.
 */
public class DraggableLinearLayoutTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private static final String TAG = "DraggableLinearLayoutTouchListener";
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private LinearLayout mLinearLayout;
    // Target LinearLayout to drop child in
    private LinearLayout mTargetLinearLayout;
    private FrameLayout mTopFrameLayout;
    private DismissCallbacks mCallbacks;
    private TransferCallbacks mTransferCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
    private int mDismissAnimationRefCount = 0;
    private float mDownXY;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private Rect mDownViewRelativeRect;
    private boolean mIsIntersect = true;
    private ValueAnimator mTmpValueAnimator;
    private PendingDismissData mTmpPendingDismissData;
    private ImageView mDownViewCopy;
    private boolean mPaused;
    // -1 means trigger orientation can be x&y
    // if your LinearLayout is a child of a vertical ScrollView, you show set trigger orientation to horizontal
    private int mOrientation = -1;
    // -1 means drag orientation can be x&y
    // if set to vertical, the child can only move the translateY property.
    private int mOrientationDrag = -1;
    // whether child can exchange position when dragging
    // TODO
    private boolean mIsChildExchangeAllowed = false;

    private boolean isOnClick;
    private View mClickedChild;
    private Runnable mClickedCallback;
    // Defined in android.view.ViewConfiguration
    // Defines the duration in milliseconds we will wait to see if a touch event is a tap or a scroll. If the user does not move within this interval, it is considered to be a tap.
    private static final int TAP_TIMEOUT = 180;
    private int mTouchSlop;

    /**
     * The callback interface used by {@link SwipeDismissListViewTouchListener} to inform its client
     * about a successful dismissal of one or more list item positions.
     */
    public interface DismissCallbacks {
        /**
         * Called to determine whether the given position can be dismissed.
         */
        boolean canDismiss(int position);

        boolean dismissOnDropOutsideBounds();

        /**
         * Called when the user has indicated they she would like to dismiss one or more list item
         * positions.
         *
         * @param linearLayout The originating {@link android.widget.ListView}.
         * @param positon      An array of positions to dismiss, sorted in descending
         *                     order for convenience.
         */
        void onDismiss(LinearLayout linearLayout, int positon);

//        void onUserActionDetected();
//
//        void onUserActionDone();

//        void onAnimationBegan();
//
//        void onAnimationFinished();
    }

    public class SimpleDismissCallbacks implements DismissCallbacks {
        @Override
        public boolean canDismiss(int position) {
            return true;
        }

        @Override
        public boolean dismissOnDropOutsideBounds() {
            return true;
        }

        @Override
        public void onDismiss(LinearLayout linearLayout, int positon) {

        }
    }

    public interface TransferCallbacks {
        /**
         * Called to determine whether the given position can be dismissed.
         */
        boolean canTransfer(int position);

        int transferToPosition();

        /**
         * Called when the user has indicated they she would like to dismiss one or more list item
         * positions.
         *
         * @param linearLayout The originating {@link android.widget.ListView}.
         * @param position     An array of positions to dismiss, sorted in descending
         *                     order for convenience.
         */
        void onTransfer(LinearLayout linearLayout, LinearLayout targetLinearLayout, int position, int toPosition);

        void onClick(LinearLayout linearLayout, View view, int position);
    }

    public class SimpleTransferCallbacks implements TransferCallbacks {

        @Override
        public boolean canTransfer(int position) {
            return true;
        }

        @Override
        public int transferToPosition() {
            return 0;
        }

        @Override
        public void onTransfer(LinearLayout linearLayout, LinearLayout targetLinearLayout, int position, int toPosition) {

        }

        @Override
        public void onClick(LinearLayout linearLayout, View view, int position) {

        }
    }

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param linearLayout The list view whose items should be dismissable.
     * @param callbacks    The callback to trigger when the user has indicated that she would like to
     *                     dismiss one or more list items.
     */
    public DraggableLinearLayoutTouchListener(LinearLayout linearLayout, FrameLayout topFrameLayout, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(linearLayout.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = linearLayout.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mLinearLayout = linearLayout;
        mCallbacks = callbacks;
        mOrientation = linearLayout.getOrientation();
        mTopFrameLayout = topFrameLayout;
        setLayoutTransition(mLinearLayout);
        mOrientationDrag = -1;
        mIsChildExchangeAllowed = true;
    }

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param linearLayout The list view whose items should be dismissable.
     * @param callbacks    The callback to trigger when the user has indicated that she would like to
     *                     dismiss one or more list items.
     */
    public DraggableLinearLayoutTouchListener(LinearLayout linearLayout, FrameLayout topFrameLayout, LinearLayout targetLinearLayout, TransferCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(linearLayout.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = linearLayout.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mLinearLayout = linearLayout;
        mTransferCallbacks = callbacks;
        mCallbacks = new SimpleDismissCallbacks() {
            @Override
            public boolean canDismiss(int position) {
                return mTransferCallbacks.canTransfer(position);
            }

            @Override
            public boolean dismissOnDropOutsideBounds() {
                return false;
            }
        };
        mOrientation = linearLayout.getOrientation();
        mTopFrameLayout = topFrameLayout;
        mTargetLinearLayout = targetLinearLayout;
        setLayoutTransition(mLinearLayout);
        setLayoutTransition(mTargetLinearLayout);
        mOrientationDrag = -1;
        mIsChildExchangeAllowed = true;
    }

    private void setLayoutTransition(ViewGroup viewGroup) {
        if (Build.VERSION.SDK_INT >= 16) {
            // 14+ will crash when we remove child, because we cannot disable APPEARING & DISAPPEARING
            // it's a big joke
            // TODO: work around it
            LayoutTransition lt = new LayoutTransition();
            if (Build.VERSION.SDK_INT >= 16) {
                lt.disableTransitionType(LayoutTransition.APPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGING);
                lt.disableTransitionType(LayoutTransition.DISAPPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            }
            viewGroup.setLayoutTransition(lt);
        }
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    private void removeTapCallback() {
        if (mClickedChild != null) {

            if (mClickedChild.isPressed()) {
                mClickedChild.setPressed(false);
            }
        }
        if (mClickedCallback != null)
            mLinearLayout.removeCallbacks(mClickedCallback);
        mClickedChild = null;
        mClickedCallback = null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mOrientation == LinearLayout.VERTICAL ? mLinearLayout.getWidth() : mLinearLayout.getHeight();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL: {
                removeTapCallback();
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mLinearLayout.getChildCount();
                int[] listViewCoords = new int[2];
                mLinearLayout.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = mLinearLayout.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y) && child.isEnabled()) {
                        mDownView = child;
                        mDownViewRelativeRect = ViewUtil.getRelativeRect(mDownView, mTopFrameLayout);
                        Log.d(TAG, "ACTION_DOWN : mDownViewRelativeRect = " + mDownViewRelativeRect);

                        // we handle the click event on children
                        isOnClick = true;
                        mClickedChild = child;
                        mClickedCallback = new Runnable() {
                            @Override
                            public void run() {
                                if (mClickedChild != null && isOnClick && mClickedChild.isEnabled()) {
                                    if (!mClickedChild.isPressed()) {
                                        mClickedChild.setPressed(true);
                                    }
                                }
                            }
                        };
                        mLinearLayout.postDelayed(mClickedCallback, TAP_TIMEOUT);
                        break;
                    }
                }

                if (mDownView != null) {
                    mDownXY = getRawXY(motionEvent);
                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();
                    Log.d(TAG, "ACTION_DOWN : mDownXY = " + mDownXY);
                    //mDownPosition = mLinearLayout.getPositionForView(mDownView);
                    int count = mLinearLayout.getChildCount();
                    for (int i = 0; i < count; i++) {
                        if (mLinearLayout.getChildAt(i) == mDownView) {
                            mDownPosition = i;
                            break;
                        }
                    }
                    // if (mCallbacks.canDismiss(mDownPosition)) {
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(motionEvent);
                    copyDownView(mDownView);
                    // } else {
                    //     mDownView = null;
                    // }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                float deltaXY = getRawXY(motionEvent) - mDownXY;

                removeTapCallback();

                if (!mSwiping) {
                    if (isOnClick) {
                        // Find the child view that was touched (perform a hit test)
                        Rect rect = new Rect();
                        int childCount = mLinearLayout.getChildCount();
                        int[] listViewCoords = new int[2];
                        mLinearLayout.getLocationOnScreen(listViewCoords);
                        int x = (int) motionEvent.getRawX() - listViewCoords[0];
                        int y = (int) motionEvent.getRawY() - listViewCoords[1];
                        View child;
                        for (int i = 0; i < childCount; i++) {
                            child = mLinearLayout.getChildAt(i);
                            child.getHitRect(rect);
                            if (rect.contains(x, y) && child.isEnabled()) {
                                Log.d(TAG, "ACTION_UP : onClick detected");
                                final View v = child;
                                final int p = i;
                                child.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        handleOnClick(v, p);
                                    }
                                });
                                break;
                            }
                        }
                        isOnClick = false;
                    }
                } else {
                    mVelocityTracker.addMovement(motionEvent);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = mVelocityTracker.getXVelocity();
                    float velocityY = mVelocityTracker.getYVelocity();
                    float velocityXY = mOrientation == LinearLayout.VERTICAL ? velocityX : velocityY;
                    float absVelocityX = mOrientation == LinearLayout.VERTICAL ? Math.abs(velocityX) : Math.abs(velocityY);
                    float absVelocityY = mOrientation == LinearLayout.VERTICAL ? Math.abs(velocityY) : Math.abs(velocityX);
                    float absVelocityXY = Math.abs(velocityXY);

                    // calculate mDownViewCopy's final Rect
                    Rect mDownViewCopyRect = new Rect(mDownViewRelativeRect);
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDownViewCopy.getLayoutParams();
                    // adjust
                    if (mOrientationDrag == LinearLayout.VERTICAL || mOrientationDrag == LinearLayout.HORIZONTAL) {
                        if (mOrientation == LinearLayout.VERTICAL)
                            mDownViewCopyRect.offset((int) deltaXY - lp.leftMargin, 0);
                        else
                            mDownViewCopyRect.offset(0, (int) deltaXY - lp.topMargin);
                    } else if (mOrientationDrag == -1) {
                        mDownViewCopyRect.offset((int) deltaX, (int) deltaY);
                    }

                    // check if can dismiss
                    boolean dismiss = false;
                    boolean isFastFlinging = false;
                    // condition one: dismiss is allowed
                    if (mCallbacks.canDismiss(mDownPosition)) {
                        Log.d(TAG, "ACTION_UP : absVelocityXY = " + absVelocityXY + " mMinFlingVelocity = " + mMinFlingVelocity);
                        // condition two: dismiss on fast flinging
                        if (mOrientationDrag == LinearLayout.VERTICAL || mOrientationDrag == LinearLayout.HORIZONTAL) {
                            if (absVelocityXY > mMinFlingVelocity) {
                                dismiss = true;
                            }
                        } else if (mOrientationDrag == -1) {
                            if (absVelocityX > mMinFlingVelocity || absVelocityY > mMinFlingVelocity) {
                                dismiss = true;
                            }
                        }
                        isFastFlinging = dismiss;

                        // condition three: dismiss on drop outside bounds
                        if (!dismiss) {
                            if (mCallbacks.dismissOnDropOutsideBounds()) {
                                dismiss = !Rect.intersects(mDownViewRelativeRect, mDownViewCopyRect);
//                            Log.d(TAG, "ACTION_UP : mDownViewRelativeRect = " + mDownViewRelativeRect);
//                            Log.d(TAG, "ACTION_UP : mDownViewCopyRect = " + mDownViewCopyRect);
//                            Log.d(TAG, "ACTION_UP : overlap? = " + Rect.intersects(mDownViewRelativeRect, mDownViewCopyRect));
                            }
                        }
                    }

                    // animate when dismiss or not
                    final View downView = mDownView; // mDownView gets null'd before animation ends
                    final View downViewCopy = mDownViewCopy; // mDownViewCopy gets null'd before animation ends
                    final int downPosition = mDownPosition;
                    if (dismiss) {
                        // transfer on fast flinging
                        boolean transfer = isFastFlinging && (mTransferCallbacks != null);
                        if (transfer) {
                            // calculate the offset x&y
                            final int toPosition = mTransferCallbacks.transferToPosition();
                            final View relativeView;
                            if (toPosition <= mTargetLinearLayout.getChildCount()) {
                                relativeView = mTargetLinearLayout.getChildAt(toPosition - 1);
                            } else {
                                relativeView = mTargetLinearLayout;
                            }
                            Point targetPoint = ViewUtil.getRelativeLocation(relativeView, mTopFrameLayout);
                            mDownViewCopyRect.offset(-(int) deltaX, (int) -deltaY);
                            final int offsetX = targetPoint.x - mDownViewCopyRect.left;
                            final int offsetY = targetPoint.y - mDownViewCopyRect.top;
//                       TODO: animate smoothly use two steps
                            //final int offsetY = targetPoint.y - mDownViewCopyRect.top - relativeView.getHeight();
                            //final float animateTimeScale = offsetY != 0 ? mAnimationTime * 1.0f / offsetY : 1;
//                        Log.d(TAG, "ACTION_UP : mDownViewCopyRect = " + mDownViewCopyRect);
//                        Log.d(TAG, "ACTION_UP : targetPoint = " + targetPoint);
//                        Log.d(TAG, "ACTION_UP : transferX = " + offsetX);
//                        Log.d(TAG, "ACTION_UP : transferY = " + offsetY);

                            // disable downView's user interactive, avoid user fast drag multi-times,
                            // and reset on animation end.
                            downView.setEnabled(false);

                            // transfer animation step one:
                            ViewUtil.safeRemoveChildView(mLinearLayout, downView);
                            setDownViewVisibility(downView, View.INVISIBLE);
                            ViewUtil.safeAddChildView(mTargetLinearLayout, downView, toPosition);
                            mDownViewCopy.animate()
                                    .translationX(offsetX)
                                    .translationY(offsetY)
                                    .setDuration(mAnimationTime)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            downView.setEnabled(true);
//                                            ViewUtil.safeRemoveChildView(mLinearLayout, downView);
                                            ViewUtil.safeRemoveChildView(mTopFrameLayout, downViewCopy);
                                            downView.setVisibility(View.VISIBLE);
                                            //ViewUtil.safeAddChildView(mTargetLinearLayout, downView, toPosition);
                                            //updateOnClickListener();
                                            mTransferCallbacks.onTransfer(mLinearLayout, mTargetLinearLayout, downPosition, toPosition);

//                                         TODO: transfer animation step two:
//                                        int finalOffsetY = offsetY + relativeView.getHeight();
//                                        // transfer animation two:
//                                        // animate to the final target place
//                                        downViewCopy.animate()
//                                                .translationY(finalOffsetY)
//                                                .setDuration((long) (animateTimeScale * relativeView.getHeight() * 2))
//                                                .setListener(new AnimatorListenerAdapter() {
//                                                    @Override
//                                                    public void onAnimationEnd(Animator animation) {
//                                                        //downView.setVisibility(View.VISIBLE);
//                                                        //downView.setVisibility(View.VISIBLE);
//                                                        downView.setVisibility(View.VISIBLE);
//                                                        ViewUtil.safeRemoveChildView(mTopFrameLayout, downViewCopy);
//                                                        mTransferCallbacks.onTransfer(mLinearLayout, mTargetLinearLayout, downPosition, toPosition);
//                                                    }
//                                    }
//                                    );
                                        }
                                    });
                        } else {
                            // dismiss like alpha --> 0
                            // disable downView's user interactive, avoid user fast drag multi-times,
                            // and reset on animation end.
                            downView.setEnabled(false);

                            //++mDismissAnimationRefCount;
                            ViewPropertyAnimator animator = mDownViewCopy.animate()
                                    .alpha(0)
                                    .setDuration(mAnimationTime)
                                    .setListener(new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            downView.setEnabled(true);
                                            ViewUtil.safeRemoveChildView(mLinearLayout, downView);
                                            ViewUtil.safeRemoveChildView(mTopFrameLayout, downViewCopy);
                                            mCallbacks.onDismiss(mLinearLayout, downPosition);
                                        }
                                    });


                            // dismiss with velocity if fast flinging
                            if (isFastFlinging) {
                                if (mOrientationDrag == LinearLayout.VERTICAL || mOrientationDrag == LinearLayout.HORIZONTAL) {
                                    animator.translationY(velocityXY);
                                } else if (mOrientationDrag == -1) {
                                    animator.translationX(velocityX).translationY(velocityY);
                                }
                            }
                        }
                    } else {
                        // animate back to the original place
                        setDownViewVisibility(downView, View.INVISIBLE);
                        mDownViewCopy.animate()
                                .translationX(0)
                                .translationY(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        //downView.setVisibility(View.VISIBLE);
                                        setDownViewVisibility(downView, View.VISIBLE);
                                        //ViewUtil.safeAddChildView(mLinearLayout, downView, downPosition);
                                        ViewUtil.safeRemoveChildView(mTopFrameLayout, downViewCopy);
                                    }
                                });
                    }
                }

                // reset
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownXY = 0;
                mDownView = null;
                mDownViewCopy = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                // check is swiping
                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                float deltaXY = getRawXY(motionEvent) - mDownXY;

                // check whether cancel on click
                if (isOnClick && (deltaX > mSlop || deltaY > mSlop)) {
                    Log.d(TAG, "ACTION_MOVE: movement detected");
                    isOnClick = false;
                    removeTapCallback();
                }

                //Log.d(TAG, "ACTION_MOVE : deltaX = " + deltaX);
                boolean isSwipingBef = mSwiping;
                // adjust
                if (mOrientationDrag == LinearLayout.VERTICAL || mOrientationDrag == LinearLayout.HORIZONTAL) {
                    //  you can only swipe on single direction
                    if (Math.abs(deltaXY) > mSlop) {
                        mSwiping = true;
                    }
                } else if (mOrientationDrag == -1) {
                    // you can swipe on both direction
                    if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                        mSwiping = true;
                    }
                }

                // indeed swiping at the first time
                if (!isSwipingBef && mSwiping) {
                    mLinearLayout.requestDisallowInterceptTouchEvent(true);

                    // Cancel ListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mLinearLayout.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();

                    // first add copy view
                    if (ViewUtil.safeIndexOfChild(mTopFrameLayout, mDownViewCopy) == -1) {
                        addDownViewCopyToTop(mDownView);
                        setDownViewVisibility(mDownView, View.INVISIBLE);
                    }
                }

                // update mDownViewCopy's position
                if (mSwiping) {
                    Rect mDownViewCopyRect = new Rect(mDownViewRelativeRect);
                    // adjust
                    if (mOrientationDrag == LinearLayout.VERTICAL || mOrientationDrag == LinearLayout.HORIZONTAL) {
                        //  you can only drag on single direction
                        if (mOrientation == LinearLayout.VERTICAL) {
                            mDownViewCopy.setTranslationX(deltaXY);
                        } else {
                            mDownViewCopy.setTranslationY(deltaXY);
                        }

                        // calculate mDownViewCopy's rect
                        if (mOrientation == LinearLayout.VERTICAL)
                            mDownViewCopyRect.offset((int) deltaXY, 0);
                        else
                            mDownViewCopyRect.offset(0, (int) deltaXY);
                    } else if (mOrientationDrag == -1) {
                        // you can drag on both direction
                        mDownViewCopy.setTranslationX(deltaX);
                        mDownViewCopy.setTranslationY(deltaY);
                        // calculate mDownViewCopy's rect
                        mDownViewCopyRect.offset((int) deltaX, (int) deltaY);
                    }

                    //Point mDownViewCopyRect = ViewUtil.getRelativeLocation(mDownViewCopy, mTopFrameLayout);
//                    Log.d(TAG, "ACTION_MOVE : mDownViewRelativeRect = " + mDownViewRelativeRect);
//                    Log.d(TAG, "ACTION_MOVE : mDownViewCopyRect = " + mDownViewCopyRect);
//                    Log.d(TAG, "ACTION_MOVE : overlap? = " + Rect.intersects(mDownViewRelativeRect, mDownViewCopyRect));

                    // animate when intersect or not
                    setIsIntersect(Rect.intersects(mDownViewRelativeRect, mDownViewCopyRect));

                    // TODO: swipe child's position when drag
//                    boolean isIntersect = Rect.intersects(mDownViewRelativeRect, mDownViewCopyRect);
//                    if (isIntersect) {
//                        int half = (mDownViewRelativeRect.right - mDownViewRelativeRect.left) / 2 + mDownViewRelativeRect.left;
//                        if (mDownViewCopyRect.left > half) {
//                            int index = ViewUtil.safeIndexOfChild(mLinearLayout, mDownView);
//                            if (index + 1 < mLinearLayout.getChildCount()) {
//                                View rightView = mLinearLayout.getChildAt(index + 1);
//                                removeLayoutTransition();
//                                mLinearLayout.removeView(mDownView);
//                                setLayoutTransition();
//                                mLinearLayout.addView(mDownView, index + 1);
//                            }
//                        }
//                    } else {
//                        setIsIntersect(isIntersect);
//                    }
                    return true;
                }
                break;
            }
        }

        return false;
    }

    private void handleOnClick(View view, int position) {
        doTransferOnSimpleClick(view, position);
        if (mTransferCallbacks != null)
            mTransferCallbacks.onClick(mLinearLayout, view, position);
    }

    public void doTransferOnSimpleClick(final View downView, final int downPosition) {
        boolean transfer = (mTransferCallbacks != null && mTransferCallbacks.canTransfer(downPosition));
        if (transfer) {
            Rect mDownViewRelativeRect = ViewUtil.getRelativeRect(downView, mTopFrameLayout);
            copyDownView(downView);
            addDownViewCopyToTop(downView);
            setDownViewVisibility(downView, View.INVISIBLE);

            final View downViewCopy = mDownViewCopy;
            // calculate the offset x&y
            final int toPosition = mTransferCallbacks.transferToPosition();
            final View relativeView;
            if (toPosition < mTargetLinearLayout.getChildCount()) {
                relativeView = mTargetLinearLayout.getChildAt(toPosition);
            } else {
                relativeView = mTargetLinearLayout;
            }
            Point targetPoint = ViewUtil.getRelativeLocation(relativeView, mTopFrameLayout);
            final int offsetX = targetPoint.x - mDownViewRelativeRect.left;
            final int offsetY = targetPoint.y - mDownViewRelativeRect.top;
//            TODO: animate smoothly use two steps
//            final int offsetY = targetPoint.y - mDownViewCopyRect.top - relativeView.getHeight();
//            final float animateTimeScale = offsetY != 0 ? mAnimationTime * 1.0f / offsetY : 1;

            // disable downView's user interactive, avoid user fast drag multi-times,
            // and reset on animation end.
            downView.setEnabled(false);

            // transfer animation step one:
            downViewCopy.animate()
                    .translationX(offsetX)
                    .translationY(offsetY)
                    .setDuration(mAnimationTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            downView.setEnabled(true);
                            ViewUtil.safeRemoveChildView(mLinearLayout, downView);
                            ViewUtil.safeRemoveChildView(mTopFrameLayout, downViewCopy);
                            downView.setVisibility(View.VISIBLE);
                            ViewUtil.safeAddChildView(mTargetLinearLayout, downView, toPosition);
                            //updateOnClickListener();
                            mTransferCallbacks.onTransfer(mLinearLayout, mTargetLinearLayout, downPosition, toPosition);

//                                         TODO: transfer animation step two:
//                                        int finalOffsetY = offsetY + relativeView.getHeight();
//                                        // transfer animation two:
//                                        // animate to the final target place
//                                        downViewCopy.animate()
//                                                .translationY(finalOffsetY)
//                                                .setDuration((long) (animateTimeScale * relativeView.getHeight() * 2))
//                                                .setListener(new AnimatorListenerAdapter() {
//                                                    @Override
//                                                    public void onAnimationEnd(Animator animation) {
//                                                        //downView.setVisibility(View.VISIBLE);
//                                                        //downView.setVisibility(View.VISIBLE);
//                                                        downView.setVisibility(View.VISIBLE);
//                                                        ViewUtil.safeRemoveChildView(mTopFrameLayout, downViewCopy);
//                                                        mTransferCallbacks.onTransfer(mLinearLayout, mTargetLinearLayout, downPosition, toPosition);
//                                                    }
//                                    }
//                                    );
                        }
                    });
        }
    }

    private void copyDownView(View view) {
        View sourceView = view;
        mDownViewCopy = new ImageView(sourceView.getContext());
        //sourceView.layout(0, 0, sourceView.getLayoutParams().width, sourceView.getLayoutParams().height);
        int width = sourceView.getWidth();
        int height = sourceView.getHeight();

        Bitmap b = ViewUtil.renderViewToBitmap(sourceView);
        mDownViewCopy.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        mDownViewCopy.setImageBitmap(b);
    }

    private void setIsIntersect(boolean intersect) {
        if (mIsIntersect != intersect) {
            mIsIntersect = intersect;
            if (mIsIntersect) {
                setDownViewVisibility(mDownView, View.INVISIBLE);
                //performDismissBack(mDownView);
                //internalReAddDownView();
            } else {
                setDownViewVisibility(mDownView, View.GONE);
                //performDismiss(mDownView);
                //internalRemoveDownView();
            }
        }
    }

    int mDownViewIndex = -1;

    private void internalRemoveDownView() {
        Log.d(TAG, "will internalRemoveDownView : remove view = " + mDownView + " parent = " + mLinearLayout + " realParent = " + mDownView.getParent());
        mDownViewIndex = ViewUtil.safeIndexOfChild(mLinearLayout, mDownView);
        if (mDownViewIndex != -1) {
            Log.d(TAG, "before internalRemoveDownView : animation = " + mDownView.getAnimation() + " parent = ");
            mLinearLayout.removeView(mDownView);
            Log.d(TAG, "did internalRemoveDownView : remove view = " + mDownView + " parent = " + mLinearLayout + " realParent = " + mDownView.getParent());
        }
    }

    private void internalReAddDownView() {
        Log.d(TAG, "will internalReAddDownView : reAdd view = " + mDownView + " parent = " + mLinearLayout + " realParent = " + mDownView.getParent());
        if (mDownViewIndex != -1 && mDownView != null) {
            Log.d(TAG, "did internalReAddDownView1 : reAdd view = " + mDownView + " parent = " + mLinearLayout + " realParent = " + mDownView.getParent());
            mDownView.setVisibility(View.INVISIBLE);
            Log.d(TAG, "did internalReAddDownView2 : reAdd view = " + mDownView + " parent = " + mLinearLayout + " realParent = " + mDownView.getParent());
            ViewUtil.safeAddChildView(mLinearLayout, mDownView, mDownViewIndex);
            mDownViewIndex = -1;
        }
    }

    private void setDownViewVisibility(View downView, int visibility) {
        if (downView != null && downView.getVisibility() != visibility) {
            Log.d(TAG, "setDownViewVisibility : visibility = " + visibility);
            downView.setVisibility(visibility);
        }
    }

    // make the copy view overlapping the origin view
    private void addDownViewCopyToTop(View downView) {
        if (downView != null && mTopFrameLayout != null && mDownViewCopy != null) {
            Point point = ViewUtil.getRelativeLocation(downView, mTopFrameLayout);
            Log.d(TAG, "ACTION_ADD : mDownViewPoint = " + point);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDownViewCopy.getLayoutParams();
            lp.setMargins(point.x, point.y, 0, 0);
            lp.gravity = Gravity.LEFT | Gravity.TOP;
            mTopFrameLayout.addView(mDownViewCopy);
        }
    }

    float getRawXY(MotionEvent motionEvent) {
        return mOrientation == LinearLayout.VERTICAL ? motionEvent.getRawX() : motionEvent.getRawY();
    }

    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }

    }

    private void performDismiss(final View dismissView, final int dismissPosition) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = mOrientation == LinearLayout.VERTICAL ? dismissView.getHeight() : dismissView.getWidth();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // No active animations, process all pending dismisses.
                // Sort by descending position
                Collections.sort(mPendingDismisses);

                int[] dismissPositions = new int[mPendingDismisses.size()];
                for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                    dismissPositions[i] = mPendingDismisses.get(i).position;
                }
                //mCallbacks.onDismiss(mLinearLayout, dismissPositions);

                ViewGroup.LayoutParams lp;
                for (PendingDismissData pendingDismiss : mPendingDismisses) {
                    // Reset view presentation
                    pendingDismiss.view.setAlpha(1f);
                    lp = pendingDismiss.view.getLayoutParams();
                    if (mOrientation == LinearLayout.VERTICAL) {
                        pendingDismiss.view.setTranslationX(0);
                        lp.height = originalHeight;
                    } else {
                        pendingDismiss.view.setTranslationY(0);
                        lp.width = originalHeight;
                    }
                    pendingDismiss.view.setLayoutParams(lp);
                }

                mPendingDismisses.clear();
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (mOrientation == LinearLayout.VERTICAL) {
                    lp.height = (Integer) valueAnimator.getAnimatedValue();
                } else {
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                }
                //lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }

    private void performDismiss(final View dismissView) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = mOrientation == LinearLayout.VERTICAL ? dismissView.getHeight() : dismissView.getWidth();

        int layoutValue = mOrientation == LinearLayout.VERTICAL ? lp.height : lp.width;
        if (layoutValue == 0)
            return;

        int startValue = mViewWidth;
        if (mTmpValueAnimator != null) {
            if (mTmpValueAnimator.isRunning()) {
                startValue = (Integer) mTmpValueAnimator.getAnimatedValue();
                mTmpValueAnimator.cancel();
            }
        }

        long time = (long) ((1.0f * startValue / mViewWidth) * mAnimationTime);
        Log.d(TAG, "performDismiss : " + startValue + "  -->  " + 0 + "  dur = " + time);
        ValueAnimator animator = ValueAnimator.ofInt(startValue, 0).setDuration(time);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
//                ViewGroup.LayoutParams lp;
//                if (mTmpPendingDismissData != null) {
//                    // Reset view presentation
//                    mTmpPendingDismissData.view.setAlpha(1f);
//                    lp = mTmpPendingDismissData.view.getLayoutParams();
//                    if (mOrientation == LinearLayout.VERTICAL) {
//                        mTmpPendingDismissData.view.setTranslationX(0);
//                        lp.height = originalHeight;
//                    } else {
//                        mTmpPendingDismissData.view.setTranslationY(0);
//                        lp.width = originalHeight;
//                    }
//                    mTmpPendingDismissData.view.setLayoutParams(lp);
//                }
                mTmpValueAnimator = null;
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (mOrientation == LinearLayout.VERTICAL) {
                    lp.height = (Integer) valueAnimator.getAnimatedValue();
                } else {
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                }
                //lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mTmpPendingDismissData = new PendingDismissData(0, dismissView);
        animator.start();
        mTmpValueAnimator = animator;
    }

    private void performDismissBack(final View dismissView) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        //final int originalHeight = mOrientation == LinearLayout.VERTICAL ? dismissView.getHeight() : dismissView.getWidth();

        int layoutValue = mOrientation == LinearLayout.VERTICAL ? lp.height : lp.width;
        if (layoutValue == mViewWidth)
            return;

        int startValue = 0;
        if (mTmpValueAnimator != null) {
            if (mTmpValueAnimator.isRunning()) {
                startValue = (Integer) mTmpValueAnimator.getAnimatedValue();
                mTmpValueAnimator.cancel();
            }
        }

        long time = (long) ((1.0f * startValue / mViewWidth) * mAnimationTime);
        Log.d(TAG, "performDismissBack : " + startValue + "  -->  " + mViewWidth + "  dur = " + time);
        ValueAnimator animator = ValueAnimator.ofInt(startValue, mViewWidth).setDuration(time);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
//                ViewGroup.LayoutParams lp;
//                if (mTmpPendingDismissData != null) {
//                    // Reset view presentation
//                    mTmpPendingDismissData.view.setAlpha(1f);
//                    lp = mTmpPendingDismissData.view.getLayoutParams();
//                    if (mOrientation == LinearLayout.VERTICAL) {
//                        mTmpPendingDismissData.view.setTranslationX(0);
//                        lp.height = originalHeight;
//                    } else {
//                        mTmpPendingDismissData.view.setTranslationY(0);
//                        lp.width = originalHeight;
//                    }
//                    mTmpPendingDismissData.view.setLayoutParams(lp);
//                }
                mTmpValueAnimator = null;
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (mOrientation == LinearLayout.VERTICAL) {
                    lp.height = (Integer) valueAnimator.getAnimatedValue();
                } else {
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                }
                //lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mTmpPendingDismissData = new PendingDismissData(0, dismissView);
        animator.start();
        mTmpValueAnimator = animator;
    }
}
