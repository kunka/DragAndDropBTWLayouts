package com.kk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.kk.control.DraggableLinearLayoutTouchListener;

/**
 * Created by xj on 13-7-18.
 */
public class MainActivity extends Activity {
    // Views
    private FrameLayout rootFrameLayout;
    private View topContainer;
    private View bottomContainer;
    private View topLLContainer;
    private LinearLayout topLL;
    private LinearLayout bottomLL;
    private DraggableLinearLayoutTouchListener mTopDraggableLinearLayoutTouchListener;
    private DraggableLinearLayoutTouchListener mBottomDraggableLinearLayoutTouchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topLL = (LinearLayout) findViewById(R.id.ll_conditions);
        rootFrameLayout = (FrameLayout) findViewById(R.id.fl_root);
        topContainer = findViewById(R.id.fl_top);
        bottomLL = (LinearLayout) findViewById(R.id.ll_conditions_bottom);
        topLLContainer = findViewById(R.id.hsv_conditions);
        bottomContainer = findViewById(R.id.hsv_conditions_bottom);

        onLoaded();
    }

    public void onLoaded() {
//        SwipeDismissListViewTouchListener swipeDismissListViewTouchListener = new SwipeDismissListViewTouchListener(mListView.getRefreshableView(), new SwipeDismissListViewTouchListener.DismissCallbacks() {
//            @Override
//            public boolean canDismiss(int position) {
//                return true;
//            }
//
//            @Override
//            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
//
//            }
//        });
        //mListView.getRefreshableView().setOnScrollListener(swipeDismissListViewTouchListener.makeScrollListener());
        //mListView.getRefreshableView().setOnTouchListener(swipeDismissListViewTouchListener);

        // test swipe dismiss
//        SwipeDismissLinearLayoutTouchListener swipeDismissLinearLayoutTouchListener = new SwipeDismissLinearLayoutTouchListener(topLL,new SwipeDismissLinearLayoutTouchListener.DismissCallbacks() {
//            @Override
//            public boolean canDismiss(int position) {
//                return position != 0;
//            }
//
//            @Override
//            public void onDismiss(LinearLayout linearLayout, int[] reverseSortedPositions) {
//
//            }
//        });
//        topLL.setOnTouchListener(swipeDismissLinearLayoutTouchListener);
//        topLL.setClipChildren(false);
//        ((ViewGroup)findViewById(R.id.hsv_conditions)).setClipChildren(false);

        // test drag dismiss
//        FrameLayout rootFrameLayout = (FrameLayout) findViewById(R.id.fl_top);
//        DraggableLinearLayoutTouchListener mTopDraggableLinearLayoutTouchListener = new DraggableLinearLayoutTouchListener(topLL, rootFrameLayout, new DraggableLinearLayoutTouchListener.DismissCallbacks() {
//            @Override
//            public boolean canDismiss(int position) {
//                return position != 0;
//            }
//
//            @Override
//            public void onDismiss(LinearLayout linearLayout, int reverseSortedPositions) {
//
//            }
//        });
//        topLL.setOnTouchListener(mTopDraggableLinearLayoutTouchListener);

        // test drag and transfer
        // topLL --> bottomLL
        mTopDraggableLinearLayoutTouchListener = new DraggableLinearLayoutTouchListener(topLL, rootFrameLayout, bottomLL, new DraggableLinearLayoutTouchListener.TransferCallbacks() {
            @Override
            public boolean canTransfer(int position) {
                return position != 0 && position != 1;
            }

            @Override
            public int transferToPosition() {
                return 2;
            }

            @Override
            public void onTransfer(LinearLayout linearLayout, LinearLayout targetLinearLayout, int position, int toPosition) {
                if (toPosition < targetLinearLayout.getChildCount()) {
                    View view = targetLinearLayout.getChildAt(toPosition);
                    view.setBackgroundResource(R.drawable.condition_invalid_bg);
                }
            }

            @Override
            public void onClick(LinearLayout linearLayout, View view, int position) {
                if (position == 0) {
                    if (linearLayout.getChildCount() > 2) {
                        mTopDraggableLinearLayoutTouchListener.doTransferOnSimpleClick(linearLayout.getChildAt(2), 2);
                    }
                }
            }
        });
        topLL.setOnTouchListener(mTopDraggableLinearLayoutTouchListener);

        // bottomLL --> topLL
        mBottomDraggableLinearLayoutTouchListener = new DraggableLinearLayoutTouchListener(bottomLL, rootFrameLayout, topLL, new DraggableLinearLayoutTouchListener.TransferCallbacks() {
            @Override
            public boolean canTransfer(int position) {
                return position != 0 && position != 1;
            }

            @Override
            public int transferToPosition() {
                return 2;
            }

            @Override
            public void onTransfer(LinearLayout linearLayout, LinearLayout targetLinearLayout, int position, int toPosition) {
                if (toPosition < targetLinearLayout.getChildCount()) {
                    View view = targetLinearLayout.getChildAt(toPosition);
                    view.setBackgroundResource(R.drawable.condition_valid_bg);
                }
            }

            @Override
            public void onClick(LinearLayout linearLayout, View view, int position) {
                if (position == 0) {
                    if (linearLayout.getChildCount() > 2) {
                        mBottomDraggableLinearLayoutTouchListener.doTransferOnSimpleClick(linearLayout.getChildAt(2), 2);
                    }
                }
            }
        });
        bottomLL.setOnTouchListener(mBottomDraggableLinearLayoutTouchListener);
    }
}
