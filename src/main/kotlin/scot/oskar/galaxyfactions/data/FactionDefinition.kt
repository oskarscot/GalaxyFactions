package scot.oskar.galaxyfactions.data

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID

data class FactionData(
    val id: UUID,
    val name: String,
    val owner: UUID,
    val description: String,
)

object Faction : Table("factions") {
    val id = javaUUID("id").uniqueIndex()
    val name = varchar("name", 255).index()
    val owner = javaUUID("owner")
    val description = varchar("description", 255).default("No description.")
}

fun ResultRow.toFactionData() = FactionData(
    id = this[Faction.id],
    name = this[Faction.name],
    owner = this[Faction.owner],
    description = this[Faction.description],
)

@JvmInline
value class FactionId(val value: UUID)

class FactionRepository(private val database: Database) : Repository<FactionId, FactionData> {

    override suspend fun findById(id: FactionId): FactionData? = suspendTransaction(database) {
        Faction.selectAll()
            .where { Faction.id eq id.value }
            .map { it.toFactionData() }
            .singleOrNull()
    }

    override suspend fun findAll(): List<FactionData> = suspendTransaction(database) {
        Faction.selectAll().map { it.toFactionData() }
    }

}

class FactionService(private val factionRepository: FactionRepository, private val factionChunkRepository: FactionChunkRepository) {



}
