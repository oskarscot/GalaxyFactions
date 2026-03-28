package scot.oskar.galaxyfactions.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.EventTitleUtil
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.data.FactionId
import scot.oskar.galaxyfactions.data.FactionService
import scot.oskar.galaxyfactions.event.ChunkChangeEvent
import scot.oskar.galaxyfactions.ext.infoMessage
import scot.oskar.galaxyfactions.ext.message

class ChunkChangeEventSystem(
    private val factionService: FactionService
) : EntityEventSystem<EntityStore, ChunkChangeEvent>(ChunkChangeEvent::class.java) {

    override fun handle(
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>,
        event: ChunkChangeEvent
    ) {
        val player = chunk.getComponent(index, PlayerRef.getComponentType()) ?: return
        val world = store.externalData.world
        val chunkStore = world.chunkStore

        val previousRef = chunkStore.getChunkReference(event.previousChunkIndex)
        val currentRef = chunkStore.getChunkReference(event.currentChunkIndex)

        val previousFactionId = previousRef?.let {
            chunkStore.store.getComponent(it, FactionChunkComponent.componentType)?.factionId
        }
        val currentFactionId = currentRef?.let {
            chunkStore.store.getComponent(it, FactionChunkComponent.componentType)?.factionId
        }

        if (previousFactionId == currentFactionId) return

        when {
            currentFactionId != null -> {
                val factionName = factionService.getCached(FactionId(currentFactionId))!!.name
                player.sendMessage(infoMessage("Entering $factionName territory"))
                EventTitleUtil.showEventTitleToPlayer(
                    player,
                    message(factionName),
                    message("Entering Faction Territory"),
                    true,
                    null,
                    4f,
                    0.5f,
                    0.5f
                )
            }
            previousFactionId != null -> {
                val factionName = factionService.getCached(FactionId(previousFactionId))!!.name
                player.sendMessage(infoMessage("Leaving $factionName territory"))
                EventTitleUtil.showEventTitleToPlayer(
                    player,
                    message("Wilderness"),
                    message("Entering"),
                    true,
                    null,
                    4f,
                    0.5f,
                    0.5f
                )
            }
        }
    }

    override fun getQuery(): Query<EntityStore> = Player.getComponentType()
}
