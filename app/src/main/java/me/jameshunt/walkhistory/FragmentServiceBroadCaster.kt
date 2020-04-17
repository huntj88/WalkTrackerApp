package me.jameshunt.walkhistory

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.*

class FragmentServiceBroadCaster(private val activity: Activity) {

    private var running: (() -> Unit)? = null
    private var notRunning: (() -> Unit)? = null

    private val broadCastNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            running?.let { it() }
            running = null
            notRunning = null
        }
    }

    fun register() {
        activity.registerReceiver(this.broadCastNewMessage, IntentFilter("msgToFragment"))
    }

    fun destroy() {
        activity.unregisterReceiver(broadCastNewMessage)
    }

    suspend fun isServiceRunning(): Boolean {
        var sup = false
        ping(
            running = { sup = true },
            notRunning = { sup = false }
        )
        while(true) {
            if (sup || running == null) {
                return sup
            }
            delay(25)
        }
    }

    private suspend fun ping(running: () -> Unit, notRunning: () -> Unit) {
        this.running = running
        this.notRunning = notRunning
        sendToService("ping")

        // delay and notRunning might be set to null
        delay(1000)
        this.notRunning?.let { it() }
        this.running = null
        this.notRunning = null
    }

    private fun sendToService(command: String) {
        val msgIntent = Intent()
            .putExtra("command", command)
            .setAction("msgToService")

        activity.sendBroadcast(msgIntent)
    }
}

class ServiceBroadCaster(private val service: Service) {
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