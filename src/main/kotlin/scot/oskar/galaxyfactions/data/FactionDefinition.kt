package scot.oskar.galaxyfactions.data

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class FactionData(
    val id: UUID,
    val name: String,
    val owner: UUID,
    val description: String = "No description.",
)

object Factions : Table("factions") {
    val id = javaUUID("id")
    val name = varchar("name", 255).index()
    val owner = javaUUID("owner")
    val description = varchar("description", 255).default("No description.")

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toFactionData() = FactionData(
    id = this[Factions.id],
    name = this[Factions.name],
    owner = this[Factions.owner],
    description = this[Factions.description],
)

@JvmInline
value class FactionId(val value: UUID)

class FactionRepository(private val database: Database) : Repository<FactionId, FactionData> {

    override suspend fun findById(id: FactionId): FactionData? = suspendTransaction(database) {
        Factions.selectAll()
            .where { Factions.id eq id.value }
            .map { it.toFactionData() }
            .singleOrNull()
    }

    override suspend fun findAll(): List<FactionData> = suspendTransaction(database) {
        Factions.selectAll().map { it.toFactionData() }
    }

    suspend fun create(factionId: FactionId, owner: UUID, name: String) = suspendTransaction(database) {
        Factions.insert {
            it[Factions.name] = name
            it[Factions.id] = factionId.value
            it[Factions.owner] = owner
        }
    }

    suspend fun findByName(name: String): FactionData? = suspendTransaction(database) {
        Factions.selectAll()
            .where { Factions.name eq name }
            .map { it.toFactionData() }
            .firstOrNull()
    }

    suspend fun deleteById(factionId: FactionId): Int = suspendTransaction(database) {
            Factions.deleteWhere { Factions.id eq factionId.value }
    }

    suspend fun findByMember(memberId: UUID): FactionData? = suspendTransaction(database) {
        Factions.innerJoin(FactionMembers)
            .selectAll()
            .where { FactionMembers.id eq memberId }
            .map { it.toFactionData() }
            .firstOrNull()
    }

}

class FactionService(
    private val factionRepository: FactionRepository,
    private val factionChunkRepository: FactionChunkRepository,
) {

    private val factionCache = ConcurrentHashMap<FactionId, FactionData>()

    suspend fun createFaction(ownerUuid: UUID, name: String): FactionData {
        val factionId = FactionId(UUID.randomUUID())
        factionRepository.create(factionId, ownerUuid, name)
        return FactionData(
            id = factionId.value,
            owner = ownerUuid,
            name = name,
        ).also { factionCache[factionId] = it }
    }

    suspend fun createChunkForFaction(chunkIndexId: ChunkIndexId, factionId: FactionId): FactionChunkData {
        factionChunkRepository.createChunk(chunkIndexId, factionId)
        return FactionChunkData(
            chunkIndex = chunkIndexId.value,
            factionId = factionId.value,
        )
    }
}
