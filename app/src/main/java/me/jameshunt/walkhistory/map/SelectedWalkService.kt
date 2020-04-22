package me.jameshunt.walkhistory.map

import android.util.Log
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import me.jameshunt.walkhistory.repo.AppDatabase
import me.jameshunt.walkhistory.repo.WalkId

class SelectedWalkService(private val appDatabase: AppDatabase) {

    lateinit var emitter: ProducerScope<WalkId>
    val selectedWalkId: Flow<WalkId> = callbackFlow<WalkId?> {
        emitter = this
        awaitClose { Log.d("SelectedWalkService", "closing") }
    }.onStart {
        emit(appDatabase.walkDao().getNewestWalk()?.walkId)
    }.filterNotNull()


    suspend fun setSelected(walkId: WalkId) {
        emitter.send(walkId)
    }
}