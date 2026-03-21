package scot.oskar.galaxyfactions.system

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import scot.oskar.galaxyfactions.event.ChunkChangeEvent
import scot.oskar.galaxyfactions.event.PlayerMoveEvent
import java.util.concurrent.ConcurrentHashMap

class PlayerMovementSystem : DelayedEntitySystem<EntityStore>(0.25f) {

    private val lastPositions = ConcurrentHashMap<Ref<EntityStore>, Vector3d>()
    private val lastChunkIndexes = ConcurrentHashMap<Ref<EntityStore>, Long>()

    override fun getQuery(): Query<EntityStore?> {
        return Query.and(Player.getComponentType(), TransformComponent.getComponentType())
    }

    override fun tick(
        dt: Float,
        index: Int,
        chunk: ArchetypeChunk<EntityStore>,
        store: Store<EntityStore>,
        buffer: CommandBuffer<EntityStore>
    ) {
        val ref = chunk.getReferenceTo(index)
        val transform = chunk.getComponent(index, TransformComponent.getComponentType())!!
        val chunkIndex = ChunkUtil.indexChunkFromBlock(transform.position.x.toInt(), transform.position.z.toInt())

        val current = transform.position
        val lastPosition = lastPositions[ref]
        val lastChunkIndex = lastChunkIndexes[ref]

        if (lastPosition == null || lastPosition != current) {
            val position = current.clone()
            if (lastPosition != null) {
                val playerMove = PlayerMoveEvent(Transform(lastPosition), Transform(position), ref)
                HytaleServer.get().eventBus.dispatchFor(playerMove.javaClass).dispatch(playerMove)
            }
            lastPositions[ref] = position
        }

        if (lastChunkIndex == null || lastChunkIndex != chunkIndex) {
            if (lastChunkIndex != null) {
                val chunkChangeEvent = ChunkChangeEvent(lastChunkIndex, chunkIndex, ref)
                HytaleServer.get().eventBus.dispatchFor(chunkChangeEvent.javaClass).dispatch(chunkChangeEvent)
            }
            lastChunkIndexes[ref] = chunkIndex
        }
    }
}
