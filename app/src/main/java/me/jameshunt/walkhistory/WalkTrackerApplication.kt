package me.jameshunt.walkhistory

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import me.jameshunt.walkhistory.map.MapWrapperFragment
import me.jameshunt.walkhistory.map.MapWrapperViewModel
import me.jameshunt.walkhistory.map.WalkPickerDialog
import me.jameshunt.walkhistory.map.WalkPickerViewModel
import me.jameshunt.walkhistory.repo.AppDatabase
import me.jameshunt.walkhistory.map.SelectedWalkService
import me.jameshunt.walkhistory.track.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

class WalkTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WalkTrackerApplication)

            modules(
                appModule(applicationContext),
                trackWalkFragmentModule(),
                walkHistoryFragmentModule(),
                mapWrapperFragmentModule()
            )
        }
    }
}

fun appModule(applicationContext: Context): Module {
    return module {
        single {
            Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "walk-tracker"
            ).build()
        }

        single { SelectedWalkService(get()) }
        single { LocationServices.getFusedLocationProviderClient(applicationContext) }
        single { LocationManager(get(), get()) }
        single { LocationCollector(get()) }
    }
}

fun trackWalkFragmentModule() = module {
    scope(named<TrackWalkFragment>()) {
        viewModel { TrackWalkViewModel(get()) }
    }
}

fun walkHistoryFragmentModule() = module {
    scope(named<WalkPickerDialog>()) {
        viewModel { WalkPickerViewModel(get(), get()) }
    }
}

fun mapWrapperFragmentModule() = module {
    scope(named<MapWrapperFragment>()) {
        viewModel { MapWrapperViewModel(get(), get()) }
    }
}