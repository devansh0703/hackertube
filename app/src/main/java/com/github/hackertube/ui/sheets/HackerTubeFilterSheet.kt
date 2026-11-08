package com.github.hackertube.ui.sheets

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.setFragmentResult
import com.github.hackertube.R
import com.github.hackertube.data.HtFilterOptions
import com.github.hackertube.data.HtFilters
import com.github.hackertube.extensions.dpToPx
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * The complete HackerTube filter set: upload date, type, duration, hidden gems,
 * evergreen videos and every sort order offered by hackertube.org.
 */
class HackerTubeFilterSheet : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val filters = HtFilters.fromBundle(arguments)
        val context = requireContext()

        val selected = mutableMapOf(
            HtFilterOptions.GROUP_DATE to filters.date,
            HtFilterOptions.GROUP_TYPE to filters.type,
            HtFilterOptions.GROUP_DURATION to filters.duration,
            HtFilterOptions.GROUP_SORT to filters.sort
        )
        val features = mutableSetOf<String>().apply {
            if (filters.gems) add("gems")
            if (filters.evergreen) add("evergreen")
        }

        val scroll = NestedScrollView(context).apply {
            isFillViewport = true
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(), 8f.dpToPx(), 24f.dpToPx(), 24f.dpToPx())
        }
        scroll.addView(root)

        root.addView(TextView(context).apply {
            text = context.getString(R.string.hackertube_filters)
            textSize = 22f
            setPadding(0, 8f.dpToPx(), 0, 12f.dpToPx())
        })

        HtFilterOptions.groups.forEach { group ->
            root.addView(TextView(context).apply {
                text = group.title
                textSize = 15f
                setPadding(0, 16f.dpToPx(), 0, 8f.dpToPx())
            })

            val chipGroup = ChipGroup(context).apply {
                isSingleSelection = !group.multiSelect
                isSelectionRequired = false
            }

            group.options.forEach { option ->
                val checked = if (group.multiSelect) {
                    features.contains(option.value)
                } else {
                    selected[group.key] == option.value
                }

                val chip = Chip(context).apply {
                    text = option.label
                    isCheckable = true
                    isChecked = checked
                    tag = option.value
                    setEnsureMinTouchTargetSize(false)
                }
                chipGroup.addView(
                    chip,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            chipGroup.setOnCheckedStateChangeListener { currentGroup, _ ->
                val checkedValues = currentGroup.checkedChipIds.mapNotNull { id ->
                    currentGroup.findViewById<Chip>(id)?.tag as? String
                }

                if (group.multiSelect) {
                    features.clear()
                    features.addAll(checkedValues)
                } else {
                    selected[group.key] = checkedValues.firstOrNull().orEmpty()
                }
            }

            root.addView(chipGroup)
        }

        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            setPadding(0, 20f.dpToPx(), 0, 0)
        }

        actions.addView(MaterialButton(context).apply {
            text = context.getString(R.string.hackertube_filter_clear_all)
            setOnClickListener {
                setFragmentResult(REQUEST_KEY, HtFilters().toBundle())
                dismiss()
            }
        })

        actions.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))

        actions.addView(MaterialButton(context).apply {
            text = context.getString(R.string.hackertube_filter_apply)
            setOnClickListener {
                val result = HtFilters(
                    date = selected[HtFilterOptions.GROUP_DATE].orEmpty(),
                    type = selected[HtFilterOptions.GROUP_TYPE].orEmpty(),
                    duration = selected[HtFilterOptions.GROUP_DURATION].orEmpty(),
                    gems = features.contains("gems"),
                    evergreen = features.contains("evergreen"),
                    sort = selected[HtFilterOptions.GROUP_SORT].orEmpty()
                )
                setFragmentResult(REQUEST_KEY, result.toBundle())
                dismiss()
            }
        })

        root.addView(actions)

        return BottomSheetDialog(context).apply {
            setContentView(scroll)
        }
    }

    companion object {
        const val REQUEST_KEY = "hacker_tube_filter_request"
    }
}
