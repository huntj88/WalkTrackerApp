package me.jameshunt.walkhistory.repo

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

@Entity
data class Walk(
    @PrimaryKey(autoGenerate = true) val walkId: Int = 0,
    val json: String
)

@Entity(
    primaryKeys = ["walkId", "timestamp"], foreignKeys = [
        ForeignKey(
            entity = Walk::class,
            parentColumns = ["walkId"],
            childColumns = ["walkId"]
        )
    ]
)
data class LocationTimestamp(
    val walkId: Int,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface LocationTimestampDao {
    @Query("SELECT * FROM locationtimestamp WHERE walkId = :walkId")
    suspend fun getLocationTimestampsForWalk(walkId: Int): List<LocationTimestamp>

    @Query("SELECT * FROM locationtimestamp WHERE walkId = :walkId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getInitialLocationTimestamp(walkId: Int): LocationTimestamp

    @Insert
    suspend fun insert(data: LocationTimestamp)
}

@Dao
interface WalkDao {
    @Insert
    suspend fun startNewWalk(walk: Walk)

    @Query("SELECT * FROM walk ORDER BY walkId DESC LIMIT 1")
    suspend fun getNewestWalk(): Walk

    @Transaction
    suspend fun startAndGetNewWalk(json: String): Walk {
        startNewWalk(Walk(json = json))
        return getNewestWalk()
    }


    @Query("SELECT * FROM walk ORDER BY walkId DESC LIMIT 1")
    fun getCurrentWalk(): Flow<Walk?>

    @Query(
        """
        SELECT walkId, json, min(timestamp) AS timestamp FROM walk 
        JOIN locationtimestamp USING(walkId) 
        GROUP BY walkId 
        ORDER BY walkId DESC
        """
    )
    suspend fun getWalksWithStartTime(): List<WalkWithTime>
}

data class WalkWithTime(
    val walkId: Int,
    val json: String,
    val timestamp: Instant
)

@Database(entities = [Walk::class, LocationTimestamp::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationTimestampDao(): LocationTimestampDao
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