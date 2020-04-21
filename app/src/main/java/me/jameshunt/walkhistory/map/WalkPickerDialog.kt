package me.jameshunt.walkhistory.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import me.jameshunt.walkhistory.R
import me.jameshunt.walkhistory.repo.AppDatabase
import me.jameshunt.walkhistory.repo.WalkWithTime
import org.koin.android.viewmodel.scope.viewModel
import java.time.format.DateTimeFormatter
import org.koin.android.scope.lifecycleScope as kLifecycleScope

class WalkPickerDialog : DialogFragment() {

    private val viewModel by kLifecycleScope.viewModel<WalkPickerViewModel>(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return RecyclerView(inflater.context).apply {
            layoutManager = LinearLayoutManager(inflater.context)
            adapter = createAdapter(viewModel.walkHistory)
        }
    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.attributes = dialog?.window?.attributes?.apply {
            width = android.view.WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    private fun createAdapter(walksLiveData: LiveData<List<WalkWithTime>>): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            init {
                walksLiveData.observe(this@WalkPickerDialog) {
                    val oldCount = itemCount
                    walks = it
                    notifyItemRangeInserted(0, it.size - oldCount)
                }
            }

            var walks: List<WalkWithTime> = emptyList()
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val view = layoutInflater.inflate(R.layout.view_walk_preview, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun getItemCount(): Int = walks.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as ViewGroup).apply {
                    val walkWithTime = walks[position]

                    findViewById<TextView>(R.id.walkNumber).text =
                        "walk number: ${walkWithTime.walkId}"

                    findViewById<TextView>(R.id.walkDate).text =
                        DateTimeFormatter.ISO_DATE.format(walkWithTime.timestamp.toLocalDate())

                    findViewById<TextView>(R.id.walkTime).text =
                        DateTimeFormatter.ISO_LOCAL_TIME.format(walkWithTime.timestamp.toLocalTime())

                    findViewById<TextView>(R.id.elapsedTime).text = "TODO"

                    setOnClickListener {
                        viewModel.setSelectedWalk(walkWithTime.walkId)
                        dismiss()
                    }
                }
            }
        }
    }
}

class WalkPickerViewModel(
    private val db: AppDatabase,
    private val selectedWalkService: SelectedWalkService
) : ViewModel() {

    val walkHistory = db
        .walkDao()
        .getCurrentWalk() // whenever a new walk is added, update the list reactively
        .mapNotNull { it }
        .map { db.walkDao().getWalksWithStartTime() }
        .asLiveData(viewModelScope.coroutineContext)

    fun setSelectedWalk(walkId: Int) {
        viewModelScope.launch {
            selectedWalkService.setSelected(walkId)
        }
    }
}
