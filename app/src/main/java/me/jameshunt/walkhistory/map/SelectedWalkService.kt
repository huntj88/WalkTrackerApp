package me.jameshunt.walkhistory.map

import android.util.Log
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import me.jameshunt.walkhistory.repo.AppDatabase

class SelectedWalkService(private val appDatabase: AppDatabase) {

    lateinit var emitter: ProducerScope<Int>
    val selectedWalkId = callbackFlow<Int> {
        emitter = this
        awaitClose { Log.d("SelectedWalkService","closing") }
    }.onStart { emit(appDatabase.walkDao().getNewestWalk().walkId) }


    suspend fun setSelected(walkId: Int) {
        emitter.channel.send(walkId)
    }
}