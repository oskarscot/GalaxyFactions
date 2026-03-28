package scot.oskar.galaxyfactions.data

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID

data class FactionChunkData(
    val chunkIndex: Long, // chunk X and Z packed into a long
    val factionId: UUID,
)

object FactionChunks : Table("faction_chunks") {
    val chunkIndex = long("chunk_index")
    val factionId = javaUUID("faction_id").references(Factions.id, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(chunkIndex)
}

fun ResultRow.toFactionChunkData() = FactionChunkData(
    chunkIndex = this[FactionChunks.chunkIndex],
    factionId = this[FactionChunks.factionId],
)

@JvmInline
value class ChunkIndexId(val value: Long)

class FactionChunkRepository(private val database: Database) : Repository<ChunkIndexId, FactionChunkData> {

    override suspend fun findById(id: ChunkIndexId): FactionChunkData? = suspendTransaction(database) {
        FactionChunks.selectAll()
            .where { FactionChunks.chunkIndex eq id.value }
            .singleOrNull()
            ?.toFactionChunkData()
    }

    override suspend fun findAll(): List<FactionChunkData> = suspendTransaction(database) {
        FactionChunks.selectAll().map { it.toFactionChunkData() }
    }

    suspend fun findByFactionId(factionId: FactionId): List<FactionChunkData> = suspendTransaction(database) {
        FactionChunks.selectAll()
            .where { FactionChunks.factionId eq factionId.value }
            .map { it.toFactionChunkData() }
    }

    suspend fun createChunk(chunkIndexId: ChunkIndexId, factionId: FactionId) = suspendTransaction(database) {
        FactionChunks.insert {
            it[chunkIndex] = chunkIndexId.value
            it[FactionChunks.factionId] = factionId.value
        }
    }

    suspend fun deleteByChunkIndex(chunkIndexId: ChunkIndexId): Int = suspendTransaction(database) {
        FactionChunks.deleteWhere { chunkIndex eq chunkIndexId.value }
    }

}