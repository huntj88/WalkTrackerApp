package me.jameshunt.walkhistory.repo

import androidx.room.*
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

@Entity
data class Walk(
    @PrimaryKey(autoGenerate = true) val walkId: Int = 0,
    val json: String
)

@Entity(primaryKeys = ["walkId", "timestamp"])
data class LocationUpdate(
    val walkId: Int,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface LocationUpdateDao {
    @Query("SELECT * FROM locationupdate WHERE walkId = :walkId")
    suspend fun getLocationDataForWalk(walkId: Int): List<LocationUpdate>

    @Query("SELECT * FROM locationupdate WHERE walkId = :walkId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstLocationDataForWalk(walkId: Int): LocationUpdate

    @Insert
    suspend fun insert(users: LocationUpdate)
}

@Dao
interface WalkDao {
    @Insert
    suspend fun startNewWalk(walk: Walk)

    @Query("SELECT * FROM walk ORDER BY walkId DESC LIMIT 1")
    suspend fun getCurrentWalk(): Walk

    @Transaction
    suspend fun startAndGetNewWalk(json: String): Walk {
        startNewWalk(Walk(json = json))
        return getCurrentWalk()
    }
}

@Database(entities = [Walk::class, LocationUpdate::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationUpdateDao(): LocationUpdateDao
    abstract fun walkDao(): WalkDao
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