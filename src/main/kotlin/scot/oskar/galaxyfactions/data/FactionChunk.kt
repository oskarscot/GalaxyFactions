package scot.oskar.galaxyfactions.data

import java.util.UUID

data class FactionChunk(
    val uuid: UUID,
    val factionUUID: UUID,
    val owner: String,
    val x: Int,
    val z: Int
)
