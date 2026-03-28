package scot.oskar.galaxyfactions.command

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import scot.oskar.galaxyfactions.FactionsPlugin
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.data.ChunkIndexId
import scot.oskar.galaxyfactions.data.FactionId
import scot.oskar.galaxyfactions.ext.errorMessage
import scot.oskar.galaxyfactions.ext.futureFromSuspend
import scot.oskar.galaxyfactions.ext.successMessage

class FactionUnclaimCommand(private val plugin: FactionsPlugin) : AbstractPlayerCommand("factionunclaim", "Unclaim a chunk from your faction") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
        val chunkIndex = ChunkUtil.indexChunkFromBlock(transform.position.x.toInt(), transform.position.z.toInt())
        val chunkStore = world.chunkStore
        val chunkRef = chunkStore.getChunkReference(chunkIndex)!!

        val existing = chunkStore.store.getComponent(chunkRef, FactionChunkComponent.componentType)
        if (existing == null) {
            player.sendMessage(errorMessage("This chunk is not claimed."))
            return
        }

        plugin.scope.futureFromSuspend {
            val factionData = plugin.factionRepository.findByMember(playerRef.uuid)
            if (factionData == null) {
                player.sendMessage(errorMessage("You're not in a faction."))
                return@futureFromSuspend false
            }
            if (existing.factionId != factionData.id) {
                player.sendMessage(errorMessage("This chunk belongs to another faction."))
                return@futureFromSuspend false
            }
            if (factionData.owner != playerRef.uuid) {
                player.sendMessage(errorMessage("Only the faction owner can unclaim chunks."))
                return@futureFromSuspend false
            }
            val factionChunks = plugin.factionChunkRepository.findByFactionId(FactionId(factionData.id))
            if (factionChunks.size <= 1) {
                player.sendMessage(errorMessage("Your faction must have at least one claimed chunk."))
                return@futureFromSuspend false
            }
            plugin.factionChunkRepository.deleteByChunkIndex(ChunkIndexId(chunkIndex))
            true
        }.thenAcceptAsync { success ->
            if (!success) return@thenAcceptAsync
            world.execute {
                chunkStore.store.removeComponent(chunkRef, FactionChunkComponent.componentType)
                chunkStore.store.getComponent(chunkRef, WorldChunk.getComponentType())!!.markNeedsSaving()
                world.worldMapManager.clearImages()
                world.playerRefs
                    .map { store.getComponent(playerRef.reference!!, Player.getComponentType()) }
                    .forEach { p -> p!!.worldMapTracker.clear() }
                player.sendMessage(successMessage("Chunk unclaimed."))
            }
        }.exceptionally {
            plugin.logger.atSevere().withCause(it).log("Failed to unclaim chunk")
            player.sendMessage(errorMessage("Failed to unclaim chunk: ${it.message}"))
            return@exceptionally null
        }
    }

    override fun hasPermission(sender: CommandSender): Boolean = true
}
