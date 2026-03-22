package scot.oskar.galaxyfactions.data

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

data class FactionChunkData(
    val chunkIndex: Long, // chunk X and Z packed into a long
    val factionId: UUID,
)

object FactionChunk : Table("faction_chunks") {
    val chunkIndex = long("chunk_index")
    val factionId = javaUUID("faction_id").references(Faction.id)

    override val primaryKey = PrimaryKey(chunkIndex)
}

fun ResultRow.toFactionChunkData() = FactionChunkData(
    chunkIndex = this[FactionChunk.chunkIndex],
    factionId = this[FactionChunk.factionId],
)

@JvmInline
value class ChunkIndexId(val value: Long)

class FactionChunkRepository(private val database: Database) : Repository<ChunkIndexId, FactionChunkData> {

    override fun findById(id: ChunkIndexId): FactionChunkData? = transaction(database) {
        FactionChunk.selectAll()
            .where { FactionChunk.chunkIndex eq id.value }
            .singleOrNull()
            ?.toFactionChunkData()
    }

    override fun findAll(): List<FactionChunkData> = transaction(database) {
        FactionChunk.selectAll().map { it.toFactionChunkData() }
    }

    fun findByFactionId(factionId: FactionId): List<FactionChunkData> = transaction(database) {
        FactionChunk.selectAll()
            .where { FactionChunk.factionId eq factionId.value }
            .map { it.toFactionChunkData() }
    }

}