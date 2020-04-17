package me.jameshunt.walkhistory

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.delay

class FragmentServiceBroadCaster(private val activity: Activity) {

    private var timeAtPing = 0L
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

    private fun sendToService(command: String) {
        val msgIntent = Intent()
            .putExtra("command", command)
            .setAction("msgToService")

        activity.sendBroadcast(msgIntent)
    }

    suspend fun ping(running: () -> Unit, notRunning: () -> Unit) {
        this.running = running
        this.notRunning = notRunning
        timeAtPing = System.currentTimeMillis()
        sendToService("ping")

        // delay and notRunning might be set to null
        delay(1000)
        this.notRunning?.let { it() }
        this.running = null
        this.notRunning = null
    }
}

class ServiceBroadCaster(private val service: Service) {
    private val broadCastNewMessage = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("receive", intent.getStringExtra("command") ?: "")

            when (intent.getStringExtra("command")) {
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