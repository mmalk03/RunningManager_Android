package com.mmalk.RunningManager;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Recycler view decorator allowing to make vertical and side spaces in recycler view
 */

public class RecyclerDecorator extends RecyclerView.ItemDecoration {

    private final int verticalSpaceHeight;
    private final int sideSpace;

    public RecyclerDecorator(int verticalSpaceHeight, int sideSpace){
        this.verticalSpaceHeight = verticalSpaceHeight;
        this.sideSpace = sideSpace;
    }

    @Override
    public void getItemOffsets(Rect rect, View view, RecyclerView parent, RecyclerView.State state){

        //set left and right space
        rect.left = sideSpace;
        rect.right = sideSpace;
        //set bottom space, excluding the last element
        if(parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1){
            rect.bottom = verticalSpaceHeight;
        }
    }
}
