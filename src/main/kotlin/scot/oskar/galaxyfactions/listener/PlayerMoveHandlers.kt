package scot.oskar.galaxyfactions.listener

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.util.EventTitleUtil
import scot.oskar.galaxyfactions.data.FactionChunk
import scot.oskar.galaxyfactions.event.ChunkChangeEvent
import scot.oskar.galaxyfactions.event.PlayerMoveEvent
import java.util.UUID

private val factionChunk = FactionChunk(
    UUID.randomUUID(),
    UUID.randomUUID(),
    "oskarscot",
    -18,
    12
)

fun onPlayerMove(event: PlayerMoveEvent) {
    val store = event.ref.store
    val player = store.getComponent(event.ref, Player.getComponentType())!!
    player.sendMessage(Message.raw("You moved!!!"))
}

fun onChunkChange(event: ChunkChangeEvent) {
    val entityStore = event.ref.store
    val player = entityStore.getComponent(event.ref, Player.getComponentType())!!
    val playerRef = entityStore.getComponent(event.ref, PlayerRef.getComponentType())!!
    val oldChunkComponent = player.world!!.chunkStore.getChunkComponent(event.previousChunkIndex, WorldChunk.getComponentType())!!
    val newChunkComponent = player.world!!.chunkStore.getChunkComponent(event.currentChunkIndex, WorldChunk.getComponentType())!!

    val wasInFactionChunk = oldChunkComponent.x == factionChunk.x && oldChunkComponent.z == factionChunk.z
    val isInFactionChunk = newChunkComponent.x == factionChunk.x && newChunkComponent.z == factionChunk.z

    when {
        wasInFactionChunk && !isInFactionChunk ->
            EventTitleUtil.showEventTitleToPlayer(playerRef,
                Message.raw("Wilderness"),
                Message.raw("Entering"),
                true,
                "",
                0.2f,
                4f,
                0.2f
                )
        !wasInFactionChunk && isInFactionChunk -> {
            player.sendMessage(Message.raw("claim uuid: ${factionChunk.uuid}"))
            EventTitleUtil.showEventTitleToPlayer(playerRef,
                Message.raw("${factionChunk.owner}'s claim"),
                Message.raw("Entering"),
                true,
                "",
                0.2f,
                4f,
                0.2f
            )
        }
    }
}