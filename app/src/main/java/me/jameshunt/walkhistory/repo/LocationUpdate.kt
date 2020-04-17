package me.jameshunt.walkhistory.repo

import androidx.room.*
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

@Entity(primaryKeys = ["walkNumber", "timestamp"])
data class LocationUpdate(
    val walkNumber: Int,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface LocationUpdateDao {
    @Query("SELECT * FROM locationupdate WHERE walkNumber = :walkNumber")
    suspend fun getLocationDataForWalk(walkNumber: Int): List<LocationUpdate>

    @Insert
    suspend fun insertAll(vararg users: LocationUpdate)
}

@Database(entities = [LocationUpdate::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): LocationUpdateDao
}

class LocalDateTimeConverter {
    private val formatter = DateTimeFormatterBuilder()
        .appendInstant(3)
        .toFormatter()

    @TypeConverter
    fun toDate(dateString: String): Instant {
        return Instant.parse(dateString)
    }

    @TypeConverter
    fun toDateString(date: Instant): String {
        return formatter.format(date)
    }
}