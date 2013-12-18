DragAndDropBTWLayouts
=====================

Perform drag and drop items in Android between two Layouts.

![alt tag](http://i.imgur.com/8hnlfAj.jpg?2)

Sample Code:

```java
    // test drag and transfer
    FrameLayout rootFrameLayout;
    LinearLayout topLL;         
    LinearLayout bottomLL;
    DraggableLinearLayoutTouchListener mTopDraggableLinearLayoutTouchListener;
    DraggableLinearLayoutTouchListener mBottomDraggableLinearLayoutTouchListener;
    
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
    
