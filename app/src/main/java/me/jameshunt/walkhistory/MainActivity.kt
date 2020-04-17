package me.jameshunt.walkhistory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    val permissionManager = PermissionManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (supportFragmentManager.findFragmentByTag("track") == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, TrackWalkFragment(), "track")
                .commit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) = permissionManager.onRequestPermissionsResult(permissions, grantResults)

}
