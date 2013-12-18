package com.kk.control;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by xj on 13-7-18.
 */
public class SwipeDismissLinearLayoutTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private LinearLayout mLinearLayout;
    private DismissCallbacks mCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private boolean mPaused;
    private int mOrientation;

    /**
     * The callback interface used by {@link SwipeDismissListViewTouchListener} to inform its client
     * about a successful dismissal of one or more list item positions.
     */
    public interface DismissCallbacks {
        /**
         * Called to determine whether the given position can be dismissed.
         */
        boolean canDismiss(int position);

        /**
         * Called when the user has indicated they she would like to dismiss one or more list item
         * positions.
         *
         * @param linearLayout           The originating {@link android.widget.ListView}.
         * @param reverseSortedPositions An array of positions to dismiss, sorted in descending
         *                               order for convenience.
         */
        void onDismiss(LinearLayout linearLayout, int[] reverseSortedPositions);
    }

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param linearLayout The list view whose items should be dismissable.
     * @param callbacks    The callback to trigger when the user has indicated that she would like to
     *                     dismiss one or more list items.
     */
    public SwipeDismissLinearLayoutTouchListener(LinearLayout linearLayout, DismissCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(linearLayout.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = linearLayout.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mLinearLayout = linearLayout;
        mCallbacks = callbacks;
        mOrientation = linearLayout.getOrientation();
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mOrientation == LinearLayout.VERTICAL ? mLinearLayout.getWidth() : mLinearLayout.getHeight();
        }

        switch (motionEvent.getActionMasked()) {
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
                    if (rect.contains(x, y)) {
                        mDownView = child;
                        break;
                    }
                }

                if (mDownView != null) {
                    mDownX = getRawXY(motionEvent);
                    //mDownPosition = mLinearLayout.getPositionForView(mDownView);
                    int count = mLinearLayout.getChildCount();
                    for (int i = 0; i < count; i++) {
                        if (mLinearLayout.getChildAt(i) == mDownView) {
                            mDownPosition = i;
                            break;
                        }
                    }
                    if (mCallbacks.canDismiss(mDownPosition)) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                    } else {
                        mDownView = null;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = getRawXY(motionEvent) - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = mOrientation == LinearLayout.VERTICAL ? Math.abs(velocityX) : Math.abs(mVelocityTracker.getYVelocity());
                float absVelocityY = mOrientation == LinearLayout.VERTICAL ? Math.abs(mVelocityTracker.getYVelocity()) : Math.abs(velocityX);
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > mViewWidth / 2) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                        && absVelocityY < absVelocityX) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = (velocityX < 0) == (deltaX < 0);
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss) {
                    // dismiss
                    final View downView = mDownView; // mDownView gets null'd before animation ends
                    final int downPosition = mDownPosition;
                    ++mDismissAnimationRefCount;
                    if (mOrientation == LinearLayout.VERTICAL) {
                        mDownView.animate()
                                .translationX(dismissRight ? mViewWidth : -mViewWidth)
                                .alpha(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        performDismiss(downView, downPosition);
                                    }
                                });
                    } else {
                        mDownView.animate()
                                .translationY(dismissRight ? mViewWidth : -mViewWidth)
                                .alpha(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        performDismiss(downView, downPosition);
                                    }
                                });
                    }

                } else {
                    // cancel
                    if (mOrientation == LinearLayout.VERTICAL) {
                        mDownView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    } else {
                        mDownView.animate()
                                .translationY(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = getRawXY(motionEvent) - mDownX;
                if (Math.abs(deltaX) > mSlop) {
                    mSwiping = true;
                    mLinearLayout.requestDisallowInterceptTouchEvent(true);

                    // Cancel ListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mLinearLayout.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (mSwiping) {
                    if (mOrientation == LinearLayout.VERTICAL) {
                        mDownView.setTranslationX(deltaX);
                    } else {
                        mDownView.setTranslationY(deltaX);
                    }
                    mDownView.setAlpha(Math.max(0f, Math.min(1f,
                            1f - 0.5f * Math.abs(deltaX) / mViewWidth)));
                    mDownView.setScaleX(1.2f);
                    mDownView.setScaleY(1.2f);
                    return true;
                }
                break;
            }
        }
        return false;
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
                --mDismissAnimationRefCount;
                if (mDismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    // Sort by descending position
                    Collections.sort(mPendingDismisses);

                    int[] dismissPositions = new int[mPendingDismisses.size()];
                    for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                        dismissPositions[i] = mPendingDismisses.get(i).position;
                    }
                    mCallbacks.onDismiss(mLinearLayout, dismissPositions);

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
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (mOrientation == LinearLayout.VERTICAL) {
                    lp.height = (Integer) valueAnimator.getAnimatedValue();;
                } else {
                    lp.width = (Integer) valueAnimator.getAnimatedValue();;
                }
                //lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }
}


