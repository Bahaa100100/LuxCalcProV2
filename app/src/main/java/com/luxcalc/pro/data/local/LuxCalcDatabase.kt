package com.luxcalc.pro.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val clientNameAr: String = "",
    val projectTypeAr: String = "سكني",
    val notesAr: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "room_designs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class RoomDesignEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val roomNameAr: String,
    val spaceCategoryName: String,
    val shapeName: String,
    val lengthM: Double,
    val widthM: Double,
    val ceilingHeightM: Double,
    val areaM2: Double,
    val ceilingTypeAr: String,
    val wallColorNameAr: String,
    val wallHex: Long,
    val wallReflectance: Double,
    val ceilingColorNameAr: String,
    val ceilingHex: Long,
    val ceilingReflectance: Double,
    val floorColorNameAr: String,
    val floorHex: Long,
    val floorReflectance: Double,
    val finishTypeName: String,
    val targetLux: Double,
    val uf: Double,
    val mf: Double,
    val maintainedLux: Double,
    val totalWatts: Double,
    val totalLumens: Double,
    val totalFixturesCount: Int,
    val layoutPatternName: String,
    val spacingXM: Double,
    val spacingYM: Double,
    val wallDistXM: Double,
    val wallDistYM: Double,
    val mixedFixturesJson: String,
    val placementsJson: String,
    val openingsJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_spaces")
data class CustomSpaceEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val categoryName: String,
    val minLux: Int,
    val defaultLux: Int,
    val maxLux: Int,
    val cctAr: String,
    val defaultCctK: Int,
    val minCri: Int,
    val workplaneHeightM: Double,
    val requirementsAr: String,
    val sourceName: String,
    val referenceAr: String
)

@Entity(tableName = "custom_fixtures")
data class CustomFixtureEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val categoryName: String,
    val layerName: String,
    val wattage: Double,
    val lumens: Double,
    val beamAngleDeg: Int,
    val cctK: Int,
    val cri: Int,
    val ipRating: String,
    val mountingTypeAr: String,
    val lightSourcesCount: Int,
    val manufacturer: String,
    val modelCode: String,
    val photometricFileRef: String
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM room_designs WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeRoomsForProject(projectId: String): Flow<List<RoomDesignEntity>>

    @Query("SELECT * FROM room_designs ORDER BY updatedAt DESC")
    fun observeAllRooms(): Flow<List<RoomDesignEntity>>

    @Query("SELECT * FROM room_designs WHERE projectId = :projectId")
    suspend fun getRoomsForProjectOnce(projectId: String): List<RoomDesignEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoom(room: RoomDesignEntity)

    @Delete
    suspend fun deleteRoom(room: RoomDesignEntity)
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM custom_spaces ORDER BY nameAr ASC")
    fun observeCustomSpaces(): Flow<List<CustomSpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomSpace(space: CustomSpaceEntity)

    @Delete
    suspend fun deleteCustomSpace(space: CustomSpaceEntity)

    @Query("SELECT * FROM custom_fixtures ORDER BY nameAr ASC")
    fun observeCustomFixtures(): Flow<List<CustomFixtureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomFixture(fixture: CustomFixtureEntity)

    @Delete
    suspend fun deleteCustomFixture(fixture: CustomFixtureEntity)
}

@Database(
    entities = [
        ProjectEntity::class,
        RoomDesignEntity::class,
        CustomSpaceEntity::class,
        CustomFixtureEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LuxCalcDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: LuxCalcDatabase? = null

        fun getInstance(context: Context): LuxCalcDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LuxCalcDatabase::class.java,
                    "luxcalc_pro.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
