package com.github.hackertube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.github.hackertube.databinding.BottomSheetItemBinding
import com.github.hackertube.obj.bottomsheetitem.BottomSheetItem
import com.github.hackertube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.hackertube.ui.extensions.setDrawables
import com.github.hackertube.ui.viewholders.BottomSheetViewHolder

class BottomSheetAdapter(
    private val listener: (index: Int) -> Unit
) : ListAdapter<BottomSheetItem, BottomSheetViewHolder>(
    DiffUtilItemCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomSheetViewHolder {
        val binding = BottomSheetItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BottomSheetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomSheetViewHolder, position: Int) {
        val item = getItem(position)!!
        holder.binding.root.apply {
            val current = item.getCurrent()
            val title = item.title.getText(context)
            text = if (current != null) "$title ($current)" else title
            isSelected = item.isSelected
            setDrawables(start = item.drawable)

            setOnClickListener {
                item.onClick.invoke()
                listener.invoke(position)
            }
        }
    }
}
