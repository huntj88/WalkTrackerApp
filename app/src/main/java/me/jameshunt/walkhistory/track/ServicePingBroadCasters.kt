package me.jameshunt.walkhistory.track

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.*

/**
 * Two way broadcasts to allow fragments to "ping" the Service to see if its running
 */

class FragmentBroadCaster(private val activity: Activity) {

    private var onRunning: (() -> Unit)? = null
    private var onNotRunning: (() -> Unit)? = null

    private val broadCastNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onRunning?.let { it() }
            onRunning = null
            onNotRunning = null
        }
    }

    fun register() {
        activity.registerReceiver(this.broadCastNewMessage, IntentFilter("msgToFragment"))
    }

    fun destroy() {
        activity.unregisterReceiver(broadCastNewMessage)
    }

    suspend fun isServiceRunning(): Boolean {
        var isRunning = false
        ping(
            running = { isRunning = true },
            notRunning = { isRunning = false }
        )
        while (true) {
            if (isRunning || onRunning == null) {
                return isRunning
            }
            delay(25)
        }
    }

    private suspend fun ping(running: () -> Unit, notRunning: () -> Unit) {
        onRunning = running
        onNotRunning = notRunning
        sendToService("ping")

        // delay so that onNotRunning might be set to null, and not be run
        // if null, then onRunning() was called
        delay(1000)
        onNotRunning?.let { it() }
        onRunning = null
        onNotRunning = null
    }

    private fun sendToService(command: String) {
        val msgIntent = Intent()
            .putExtra("command", command)
            .setAction("msgToService")

        activity.sendBroadcast(msgIntent)
    }
}

class AndroidServiceBroadCaster(private val service: Service) {
    private val broadCastNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent
                .getStringExtra("command")
                ?: throw IllegalArgumentException("Missing command")

            Log.d("receive", command)

            when (command) {
                "ping" -> sendToFragment("ping")
                else -> TODO()
            }
        }
    }

    fun register() {
        service.registerReceiver(this.broadCastNewMessage, IntentFilter("msgToService"))
    }

    fun destroy() {
        service.unregisterReceiver(broadCastNewMessage)
    }

    fun sendToFragment(command: String) {
        val msgIntent = Intent()
            .putExtra("command", command)
            .setAction("msgToFragment")

        service.sendBroadcast(msgIntent)
    }
}