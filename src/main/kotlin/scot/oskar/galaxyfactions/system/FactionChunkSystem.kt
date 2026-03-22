package scot.oskar.galaxyfactions.system

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefChangeSystem
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import scot.oskar.galaxyfactions.component.FactionChunkComponent

class FactionChunkSystem(): RefChangeSystem<ChunkStore, FactionChunkComponent>() {

    override fun componentType(): ComponentType<ChunkStore, FactionChunkComponent> = FactionChunkComponent.componentType

    override fun onComponentAdded(
        ref: Ref<ChunkStore>,
        component: FactionChunkComponent,
        store: Store<ChunkStore>,
        buffer: CommandBuffer<ChunkStore>
    ) {
        TODO("Not yet implemented")
    }

    override fun onComponentSet(
        ref: Ref<ChunkStore>,
        oldComponent: FactionChunkComponent?,
        newComponent: FactionChunkComponent,
        store: Store<ChunkStore>,
        buffer: CommandBuffer<ChunkStore>
    ) {
        TODO("Not yet implemented")
    }

    override fun onComponentRemoved(
        ref: Ref<ChunkStore>,
        component: FactionChunkComponent,
        store: Store<ChunkStore>,
        buffer: CommandBuffer<ChunkStore>
    ) {
        TODO("Not yet implemented")
    }

    override fun getQuery(): Query<ChunkStore> = FactionChunkComponent.componentType

}