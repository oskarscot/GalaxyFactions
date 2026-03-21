package scot.oskar.galaxyfactions.command

import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.BsonUtil
import org.bson.BsonDocument
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class DumpChunkCommand : AbstractPlayerCommand("dumpchunk", "Dump the chunk you're standing in as JSON (Admin)") {

    protected override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val transform: TransformComponent? =
            store.getComponent(ref, TransformComponent.getComponentType())
        if (transform == null) {
            return
        }

        val x: Double = transform.getPosition().getX()
        val z: Double = transform.getPosition().getZ()
        val chunkIndex: Long = ChunkUtil.indexChunkFromBlock(x, z)
        val cx: Int = ChunkUtil.xOfChunkIndex(chunkIndex)
        val cz: Int = ChunkUtil.zOfChunkIndex(chunkIndex)

        val chunkStore: ChunkStore = world.getChunkStore()
        val chunkRef: Ref<ChunkStore?>? = chunkStore.getChunkReference(chunkIndex)
        if (chunkRef == null) {
            return
        }

        val chunkComponentStore: Store<ChunkStore?> = chunkStore.getStore()
        val holder: Holder<ChunkStore?>? = chunkComponentStore.copySerializableEntity(chunkRef)
        if (holder == null) {
            return
        }

        val doc: BsonDocument? = ChunkStore.REGISTRY.serialize(holder)
        val json: String? = BsonUtil.toJson(doc)

        val fileName = "chunk_" + world.getName() + "_" + cx + "_" + cz + ".json"
        val outputPath: Path = Path.of(fileName)
        try {
            Files.writeString(outputPath, json)
        } catch (e: IOException) {
            return
        }

        playerRef.sendMessage(Message.raw("Dumped chunk (" + cx + ", " + cz + ") to " + outputPath.toAbsolutePath()))
    }

    public override fun hasPermission(sender: CommandSender): Boolean {
        return true
    }
}