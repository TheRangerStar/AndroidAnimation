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
    val hue: Float
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

@Database(entities = [AttractorSetting::class], version = 1)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
