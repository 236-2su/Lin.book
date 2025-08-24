package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

abstract class SwipeToDeleteCallback(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
    private val background = ColorDrawable()
    private val backgroundColor = Color.parseColor("#C50000")

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false // 드래그 앤 드롭을 사용하지 않음
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        // 배경 그리기
        background.color = backgroundColor
        background.setBounds(
            if (dX > 0) itemView.left else itemView.right + dX.toInt(),
            itemView.top,
            if (dX > 0) itemView.left + dX.toInt() else itemView.right,
            itemView.bottom
        )
        background.draw(c)

        // 아이콘 위치 계산
        val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val iconMargin = (itemHeight - intrinsicHeight) / 2
        val iconLeft = if (dX > 0) itemView.left + iconMargin else itemView.right - iconMargin - intrinsicWidth
        val iconRight = if (dX > 0) itemView.left + iconMargin + intrinsicWidth else itemView.right - iconMargin
        val iconBottom = iconTop + intrinsicHeight

        // 아이콘 그리기
        deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        deleteIcon?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
