package scot.oskar.galaxyfactions.event

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.event.IEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class ChunkChangeEvent(
    val previousChunkIndex: Long,
    val currentChunkIndex: Long,
    val ref: Ref<EntityStore>
) : IEvent<Void>