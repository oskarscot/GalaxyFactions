package scot.oskar.galaxyfactions.data

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.util.UUID

data class FactionMemberData(
    val id: UUID,
    val factionId: FactionId,
)

object FactionMembers : Table("faction_members") {
    val id = javaUUID("id")
    val factionId = javaUUID("faction_id").references(Factions.id, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toFactionMemberData() = FactionMemberData(
    id = this[FactionMembers.id],
    factionId = FactionId(this[FactionMembers.factionId]),
)

class FactionMemberRepository(private val database: Database) : Repository<UUID, FactionMemberData> {

    override suspend fun findById(id: UUID): FactionMemberData? = suspendTransaction(database) {
        FactionMembers.selectAll()
            .where { FactionMembers.id eq id }
            .singleOrNull()
            ?.toFactionMemberData()
    }

    override suspend fun findAll(): List<FactionMemberData> = suspendTransaction(database) {
        FactionMembers.selectAll().map { it.toFactionMemberData() }
    }

    suspend fun findByFactionId(factionId: FactionId): List<FactionMemberData> = suspendTransaction(database) {
        FactionMembers.selectAll()
            .where { FactionMembers.factionId eq factionId.value }
            .map { it.toFactionMemberData() }
    }

    suspend fun addMemberToFaction(factionId: FactionId, member: UUID) = suspendTransaction(database) {
        FactionMembers.insert {
            it[FactionMembers.factionId] = factionId.value
            it[FactionMembers.id] = member
        }
    }

}
