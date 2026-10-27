package com.juani.careervault

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "careers")
data class Career(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dt: String,
    val created: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stints",
    foreignKeys = [ForeignKey(entity = Career::class, parentColumns = ["id"], childColumns = ["careerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("careerId")]
)
data class Stint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val careerId: Long,
    val kind: String,            // CLUB | NT (selección)
    val name: String,
    val crest: String = "",
    val yearFrom: String = "",
    val yearTo: String = ""
)

@Entity(
    tableName = "awards",
    foreignKeys = [ForeignKey(entity = Career::class, parentColumns = ["id"], childColumns = ["careerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("careerId")]
)
data class Award(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val careerId: Long,
    val name: String,
    val club: String = "",
    val season: String = ""
)

@Entity(
    tableName = "rivals",
    foreignKeys = [ForeignKey(entity = Stint::class, parentColumns = ["id"], childColumns = ["stintId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("stintId")]
)
data class Rival(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stintId: Long,
    val name: String,
    val crest: String = ""
)

@Entity(
    tableName = "stat_players",
    foreignKeys = [ForeignKey(entity = Stint::class, parentColumns = ["id"], childColumns = ["stintId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("stintId")]
)
data class StatPlayer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stintId: Long,
    val name: String,
    val gk: Boolean = false
)

// Registro genérico: TRANSFER, TITLE, FINAL, DERBY (refId = rival), PROMOTED, RETIRED, STAT (refId = jugador)
@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(entity = Stint::class, parentColumns = ["id"], childColumns = ["stintId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("stintId")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stintId: Long,
    val category: String,
    val refId: Long = 0,
    val season: String = "",
    val name: String = "",
    val type: String = "",
    val amount: Long = 0,
    val other: String = "",
    val gf: Int = 0,
    val ga: Int = 0,
    val matches: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val clean: Int = 0,
    val position: String = "",
    val extra: String = "",
    val note: String = ""
)

@Entity(tableName = "squads")
data class Squad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val formation: String
)

@Entity(
    tableName = "squad_players",
    foreignKeys = [ForeignKey(entity = Squad::class, parentColumns = ["id"], childColumns = ["squadId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("squadId")]
)
data class SquadPlayer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val squadId: Long,
    val name: String = "",
    val number: String = "",
    val pos: String = "",
    val role: String = "",
    val tag: String = "",        // "" | FUTURO | CESION
    val starter: Boolean = true,
    val x: Float = 0.5f,
    val y: Float = 0.5f
)

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,            // FICHAJE | VENTA
    val name: String,
    val position: String = ""
)

@Dao
interface CvDao {
    @Query("SELECT * FROM careers ORDER BY created DESC") fun careers(): Flow<List<Career>>
    @Query("SELECT * FROM careers WHERE id = :id") fun career(id: Long): Flow<Career?>
    @Insert suspend fun insertCareer(c: Career): Long
    @Update suspend fun updateCareer(c: Career)
    @Delete suspend fun deleteCareer(c: Career)

    @Query("SELECT * FROM stints ORDER BY yearFrom, id") fun allStints(): Flow<List<Stint>>
    @Query("SELECT * FROM stints WHERE careerId = :cid ORDER BY yearFrom, id") fun stints(cid: Long): Flow<List<Stint>>
    @Query("SELECT * FROM stints WHERE id = :id") fun stint(id: Long): Flow<Stint?>
    @Query("SELECT * FROM stints WHERE id = :id") suspend fun stintOnce(id: Long): Stint?
    @Insert suspend fun insertStint(s: Stint): Long
    @Update suspend fun updateStint(s: Stint)
    @Delete suspend fun deleteStint(s: Stint)

    @Query("SELECT * FROM awards WHERE careerId = :cid ORDER BY season DESC, id DESC") fun awards(cid: Long): Flow<List<Award>>
    @Insert suspend fun insertAward(a: Award)
    @Update suspend fun updateAward(a: Award)
    @Delete suspend fun deleteAward(a: Award)

    @Query("SELECT * FROM entries WHERE stintId = :sid AND category = :cat ORDER BY id DESC") fun entries(sid: Long, cat: String): Flow<List<Entry>>
    @Query("SELECT * FROM entries WHERE stintId = :sid") fun allEntries(sid: Long): Flow<List<Entry>>
    @Query("SELECT * FROM entries WHERE refId = :ref AND category = :cat ORDER BY id DESC") fun entriesByRef(ref: Long, cat: String): Flow<List<Entry>>
    @Insert suspend fun insertEntry(e: Entry)
    @Update suspend fun updateEntry(e: Entry)
    @Delete suspend fun deleteEntry(e: Entry)
    @Query("DELETE FROM entries WHERE category = :cat AND refId = :ref") suspend fun deleteByRef(cat: String, ref: Long)

    @Query("SELECT * FROM rivals WHERE stintId = :sid ORDER BY name") fun rivals(sid: Long): Flow<List<Rival>>
    @Query("SELECT * FROM rivals WHERE id = :id") fun rival(id: Long): Flow<Rival?>
    @Query("SELECT * FROM rivals WHERE id = :id") suspend fun rivalOnce(id: Long): Rival?
    @Insert suspend fun insertRival(r: Rival): Long
    @Update suspend fun updateRival(r: Rival)
    @Delete suspend fun deleteRival(r: Rival)

    @Query("SELECT * FROM stat_players WHERE stintId = :sid ORDER BY name") fun statPlayers(sid: Long): Flow<List<StatPlayer>>
    @Query("SELECT * FROM stat_players WHERE id = :id") fun statPlayer(id: Long): Flow<StatPlayer?>
    @Insert suspend fun insertStatPlayer(p: StatPlayer): Long
    @Update suspend fun updateStatPlayer(p: StatPlayer)
    @Delete suspend fun deleteStatPlayer(p: StatPlayer)

    @Query("SELECT * FROM squads ORDER BY id DESC") fun squads(): Flow<List<Squad>>
    @Query("SELECT * FROM squads WHERE id = :id") fun squad(id: Long): Flow<Squad?>
    @Insert suspend fun insertSquad(s: Squad): Long
    @Update suspend fun updateSquad(s: Squad)
    @Delete suspend fun deleteSquad(s: Squad)
    @Query("SELECT * FROM squad_players WHERE squadId = :sid ORDER BY id") fun squadPlayers(sid: Long): Flow<List<SquadPlayer>>
    @Query("SELECT * FROM squad_players") fun allSquadPlayers(): Flow<List<SquadPlayer>>
    @Insert suspend fun insertSquadPlayer(p: SquadPlayer)
    @Update suspend fun updateSquadPlayer(p: SquadPlayer)
    @Delete suspend fun deleteSquadPlayer(p: SquadPlayer)
    @Query("UPDATE squad_players SET x = :x, y = :y WHERE id = :id") suspend fun moveSquadPlayer(id: Long, x: Float, y: Float)

    @Query("SELECT * FROM plans ORDER BY id DESC") fun plans(): Flow<List<Plan>>
    @Insert suspend fun insertPlan(p: Plan)
    @Update suspend fun updatePlan(p: Plan)
    @Delete suspend fun deletePlan(p: Plan)
}

@Database(
    entities = [Career::class, Stint::class, Award::class, Rival::class, StatPlayer::class, Entry::class, Squad::class, SquadPlayer::class, Plan::class],
    version = 1,
    exportSchema = false
)
abstract class CvDb : RoomDatabase() {
    abstract fun dao(): CvDao

    companion object {
        @Volatile private var inst: CvDb? = null
        fun get(c: Context): CvDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, CvDb::class.java, "careervault.db").build().also { inst = it }
        }
    }
}
