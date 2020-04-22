package me.jameshunt.walkhistory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*
import me.jameshunt.walkhistory.map.MapWrapperFragment
import me.jameshunt.walkhistory.track.PermissionManager
import me.jameshunt.walkhistory.track.TrackWalkFragment

class MainActivity : AppCompatActivity() {

    val permissionManager = PermissionManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> TrackWalkFragment()
                    1 -> MapWrapperFragment()
                    else -> throw IllegalStateException()
                }
            }

            override fun getItemCount(): Int {
                return 2
            }
        }

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigationView.selectedItemId = when(position) {
                    0 -> R.id.track_walk
                    1 -> R.id.map_walks
                    else -> throw NotImplementedError()
                }
            }
        })

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.track_walk -> viewPager.setCurrentItem(0, true)
                R.id.map_walks -> viewPager.setCurrentItem(1, true)
                else -> throw NotImplementedError()
            }
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) = permissionManager.onRequestPermissionsResult(permissions, grantResults)
}
