package me.jameshunt.walkhistory

import android.Manifest.*
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class PermissionManager(private val context: Activity) {
    private var onLocationGrant: ((PermissionResult) -> Unit)? = null

    fun onLocationGranted(action: (PermissionResult) -> Unit) {
        if(canUseLocation()) {
            action(PermissionResult.Granted)
        } else {
            context.showDialogIfPossible(permission.ACCESS_FINE_LOCATION)
            onLocationGrant = action
        }
    }

    fun canUseLocation(): Boolean = context.isGranted(permission.ACCESS_FINE_LOCATION)

    fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        permissions
            .indices
            .map { permissions[it] to grantResults[it] }
            .map { (permissionString, status) ->
                Log.d("permission", permissionString)
                when(permissionString) {
                    permission.ACCESS_FINE_LOCATION -> {
                        val returnStatus = when (status) {
                            PackageManager.PERMISSION_GRANTED -> PermissionResult.Granted
                            else -> PermissionResult.Denied
                        }

                        onLocationGrant?.let { it(returnStatus) }
                        onLocationGrant = null
                    }
                    else -> TODO("handle future permissions, $permissionString")
                }
            }

    }

    private fun Activity.showDialogIfPossible(permissionString: String) {
        val willShowRational = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            permissionString
        )

        when (willShowRational) {
            true -> {
                AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle("Permission necessary")
                    .setMessage("Location required to track walk")
                    .setPositiveButton("Got it") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@showDialogIfPossible,
                            arrayOf(permissionString),
                            1
                        )
                    }
                    .create()
                    .show()
            }
            false -> ActivityCompat.requestPermissions(
                this,
                arrayOf(permissionString),
                1
            )
        }
    }

    private fun Context.isGranted(permission: String): Boolean {
        val currentPermissionStatus = ContextCompat.checkSelfPermission(this, permission)
        return currentPermissionStatus == PackageManager.PERMISSION_GRANTED
    }
}

enum class PermissionResult {
    Granted,
    Denied
}

fun Activity.permissionManager(): PermissionManager {
    return (this as MainActivity).permissionManager
}