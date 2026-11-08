package com.github.hackertube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.hackertube.R
import com.github.hackertube.databinding.DialogSubscriptionGroupsBinding
import com.github.hackertube.db.obj.SubscriptionGroup
import com.github.hackertube.extensions.move
import com.github.hackertube.extensions.setOnDraggedListener
import com.github.hackertube.ui.adapters.SubscriptionGroupsAdapter
import com.github.hackertube.ui.models.SubscriptionsViewModel
import com.github.hackertube.ui.sheets.AddChannelToGroupSheet.Companion.applyGroupsDiff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelGroupsSheet : ExpandedBottomSheet(R.layout.dialog_subscription_groups) {
    private val viewModel: SubscriptionsViewModel by activityViewModels()

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogSubscriptionGroupsBinding.bind(view)
        binding.groupsRV.layoutManager = LinearLayoutManager(context)
        val groups = viewModel.groups.value.orEmpty().toMutableList()
        val adapter = SubscriptionGroupsAdapter(groups, viewModel, parentFragmentManager)
        binding.groupsRV.adapter = adapter

        binding.newGroup.setOnClickListener {
            viewModel.groupToEdit = SubscriptionGroup("", mutableListOf(), 0)
            EditChannelGroupSheet().show(parentFragmentManager, null)
        }

        viewModel.groups.observe(viewLifecycleOwner) {
            adapter.groups = viewModel.groups.value.orEmpty().toMutableList()
            lifecycleScope.launch { adapter.notifyDataSetChanged() }
        }

        binding.confirm.setOnClickListener {
            val groupsBeforeChange = viewModel.groups.value.orEmpty()

            viewModel.groups.value = adapter.groups
            viewModel.groups.value?.forEachIndexed { index, group -> group.index = index }
            CoroutineScope(Dispatchers.IO).launch {
                applyGroupsDiff(groupsBeforeChange, adapter.groups)
            }
            dismiss()
        }

        binding.groupsRV.setOnDraggedListener { from, to ->
            adapter.groups.move(from, to)
            adapter.notifyItemMoved(from, to)
        }
    }
}
