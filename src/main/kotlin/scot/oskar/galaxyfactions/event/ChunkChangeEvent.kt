package scot.oskar.galaxyfactions.event

import com.hypixel.hytale.component.system.EcsEvent

class ChunkChangeEvent(
    val previousChunkIndex: Long,
    val currentChunkIndex: Long,
) : EcsEvent()
