package scot.oskar.galaxyfactions.command

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

class TestCommand: AbstractPlayerCommand("test", "Test command") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore?>,
        ref: Ref<EntityStore?>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())!!
        val transform = store.getComponent(ref, TransformComponent.getComponentType())!!
        val chunkIndex = ChunkUtil.indexChunkFromBlock(transform.position.x.toInt(), transform.position.z.toInt())
        val chunkComponent = world.chunkStore.getChunkComponent(chunkIndex, WorldChunk.getComponentType())!!
        player.sendMessage(Message.join(
            Message.raw("You are at chunk x=${chunkComponent.x} z=${chunkComponent.z}")
        ))
    }

    override fun hasPermission(sender: CommandSender): Boolean {
        return true
    }

}