package scot.oskar.galaxyfactions.component

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableObject
import java.util.UUID

@SerializableObject
class FactionChunkComponent(
    @SerialWithCodec("Codec.LONG") var chunkIndex: Long? = null,
    @SerialWithCodec("Codec.UUID_BINARY") var factionId: UUID? = null,
): Component<ChunkStore> {

    companion object {
        lateinit var componentType: ComponentType<ChunkStore, FactionChunkComponent>
    }

    override fun clone(): Component<ChunkStore> = FactionChunkComponent(chunkIndex, factionId)

}