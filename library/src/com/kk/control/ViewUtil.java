package com.kk.control;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * Created by xj on 13-7-6.
 */
public class ViewUtil {
    /**
     * whether contains child
     *
     * @param parent
     * @param child
     * @return the index of the child if the parent contains the child, otherwise return -1
     */
    public static int safeIndexOfChild(ViewGroup parent, View child) {
        return (parent != null && child != null) ? parent.indexOfChild(child) : -1;
    }

    public static void safeRemoveChildView(ViewGroup parent, View child) {
        Log.d("", "will safeRemoveChildView : remove view = " + child + " parent = " + parent + " realParent = " + child.getParent());
        if (parent != null && child != null) {
            // if (ViewUtil.safeIndexOfChild(parent, child) != -1) {
            if (parent == child.getParent()) {
                parent.removeView(child);
                Log.d("ViewUtil", "did safeRemoveChildView : remove view = " + child + " parent = " + parent + " realParent = " + child.getParent());
            }
        }
    }

    public static void safeRemoveChildView(View child) {
        Log.d("ViewUtil", "will safeRemoveChildView : remove view = " + child);
        if (child != null) {
            ViewParent parent = child.getParent();
            if (parent != null && parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(child);
                Log.d("ViewUtil", "did safeRemoveChildView : remove view = " + child + " parent = " + parent + " realParent = " + child.getParent());
            }
        }
    }

    public static void safeAddChildView(ViewGroup parent, View child, int index) {
        Log.d("ViewUtil", "will safeAddChildView : add view = " + child + " parent = " + parent + " realParent = " + child.getParent());
        if (parent != null && child != null) {
            if (child.getParent() == null && ViewUtil.safeIndexOfChild(parent, child) == -1) {
                parent.addView(child, index);
                Log.d("ViewUtil", "did safeAddChildView : add view = " + child + " parent = " + parent + " realParent = " + child.getParent());
            }
        }
    }

    public static Bitmap renderViewToBitmap(View sourceView) {
        if (sourceView != null) {
            //sourceView.layout(0, 0, sourceView.getLayoutParams().width, sourceView.getLayoutParams().height);
            int width = sourceView.getWidth();
            int height = sourceView.getHeight();
            if (width > 0 && height > 0) {
                Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                sourceView.draw(c);
                return b;
            }
        }
        return null;
    }

    /**
     * get the relative coordinate
     *
     * @param view
     * @param relativeView
     * @return the relative location
     */
    public static Point getRelativeLocation(View view, View relativeView) {
        if (view != null && relativeView != null && view != relativeView) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int[] location2 = new int[2];
            relativeView.getLocationOnScreen(location2);
            return new Point(location[0] - location2[0], location[1] - location2[1]);
        }
        return new Point(0, 0);
    }

    /**
     * get the relative rect
     *
     * @param view
     * @param relativeView
     * @return the relative view's rect
     */
    public static Rect getRelativeRect(View view, View relativeView) {
        if (relativeView != null) {
            Point point = getRelativeLocation(view, relativeView);
            Rect rect = new Rect(point.x, point.y, point.x + view.getWidth(), point.y + view.getHeight());
            return rect;
        }
        return new Rect();
    }
}
