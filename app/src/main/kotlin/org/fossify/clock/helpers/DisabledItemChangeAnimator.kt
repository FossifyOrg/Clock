package org.fossify.clock.helpers

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Simple RecyclerView animator that disable itemChange animations
 */
class DisabledItemChangeAnimator : DefaultItemAnimator() {
    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        preInfo: ItemHolderInfo,
        postInfo: ItemHolderInfo,
    ): Boolean {
        dispatchChangeFinished(oldHolder, false)
        return false
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder?,
        newHolder: RecyclerView.ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
    ): Boolean {
        dispatchChangeFinished(oldHolder, false)
        return false
    }

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
        return true
    }

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder, payloads: MutableList<Any>): Boolean {
        return true
    }
}
