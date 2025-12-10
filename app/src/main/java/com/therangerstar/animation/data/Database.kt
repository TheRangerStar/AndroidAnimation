package com.therangerstar.animation.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "attractor_settings")
data class AttractorSetting(
    @PrimaryKey val attractorName: String,
    val hue: Float,
    val saturation: Float = 1.0f, // Default saturation
    val speed: Float = 1.0f, // Default speed
    val particleCount: Int = 8000 // Default particle count
)

@Dao
interface AttractorDao {
    @Query("SELECT * FROM attractor_settings WHERE attractorName = :name")
    fun getSetting(name: String): Flow<AttractorSetting?>

    @Query("SELECT * FROM attractor_settings WHERE attractorName = :name")
    suspend fun getSettingSync(name: String): AttractorSetting?

    @Query("SELECT * FROM attractor_settings")
    fun getAllSettings(): Flow<List<AttractorSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: AttractorSetting)
}

@Database(entities = [AttractorSetting::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attractorDao(): AttractorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
