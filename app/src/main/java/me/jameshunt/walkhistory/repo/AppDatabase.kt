package me.jameshunt.walkhistory.repo

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

typealias WalkId = Int

@Entity
data class Walk(
    @PrimaryKey(autoGenerate = true) val walkId: WalkId = 0,
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
    val walkId: WalkId,
    val timestamp: OffsetDateTime,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface LocationTimestampDao {
    @Query("SELECT * FROM locationtimestamp WHERE walkId = :walkId")
    suspend fun getLocationTimestampsForWalk(walkId: WalkId): List<LocationTimestamp>

    @Query("SELECT * FROM locationtimestamp WHERE walkId = :walkId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getInitialLocationTimestamp(walkId: WalkId): LocationTimestamp

    @Insert
    suspend fun insert(data: LocationTimestamp)
}

@Dao
interface WalkDao {
    @Insert
    suspend fun startNewWalk(walk: Walk)

    @Query("SELECT * FROM walk ORDER BY walkId DESC LIMIT 1")
    suspend fun getNewestWalk(): Walk?

    @Transaction
    suspend fun startAndGetNewWalk(json: String): Walk {
        startNewWalk(Walk(json = json))
        return getNewestWalk()!!
    }


    @Query("SELECT * FROM walk ORDER BY walkId DESC LIMIT 1")
    fun getCurrentWalk(): Flow<Walk?>

    @Query(
        """
        SELECT walkId, json, min(timestamp) AS startTime, max(timestamp) AS endTime FROM walk 
        JOIN locationtimestamp USING(walkId) 
        GROUP BY walkId 
        ORDER BY walkId DESC
        """
    )
    suspend fun getWalksWithStartTime(): List<WalkWithTime>
}

data class WalkWithTime(
    val walkId: WalkId,
    val json: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime
)

@Database(entities = [Walk::class, LocationTimestamp::class], version = 1)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationTimestampDao(): LocationTimestampDao
    abstract fun walkDao(): WalkDao
}

class LocalDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    fun toDate(dateString: String): OffsetDateTime {
        return OffsetDateTime.parse(dateString)
    }

    @TypeConverter
    fun toDateString(date: OffsetDateTime): String {
        return formatter.format(date)
    }
}