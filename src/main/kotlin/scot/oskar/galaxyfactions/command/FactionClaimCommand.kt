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

class FactionClaimCommand(private val plugin: FactionsPlugin) : AbstractPlayerCommand("factionclaim", "Claim a chunk for your faction") {

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
        if (existing != null) {
            player.sendMessage(errorMessage("This chunk is already claimed."))
            return
        }

        val chunkX = ChunkUtil.xOfChunkIndex(chunkIndex)
        val chunkZ = ChunkUtil.zOfChunkIndex(chunkIndex)

        plugin.scope.futureFromSuspend {
            val factionData = plugin.factionRepository.findByMember(playerRef.uuid)
            if (factionData == null) {
                player.sendMessage(errorMessage("You're not in a faction."))
                return@futureFromSuspend null
            }
            val factionId = FactionId(factionData.id)

            // Must border an existing faction chunk
            val hasAdjacentClaim = listOf(
                ChunkUtil.indexChunk(chunkX, chunkZ - 1),
                ChunkUtil.indexChunk(chunkX, chunkZ + 1),
                ChunkUtil.indexChunk(chunkX - 1, chunkZ),
                ChunkUtil.indexChunk(chunkX + 1, chunkZ),
            ).any { neighborIndex ->
                plugin.factionChunkRepository.findById(ChunkIndexId(neighborIndex))?.factionId == factionData.id
            }

            if (!hasAdjacentClaim) {
                player.sendMessage(errorMessage("You can only claim chunks adjacent to your faction's territory."))
                return@futureFromSuspend null
            }

            plugin.factionService.createChunkForFaction(ChunkIndexId(chunkIndex), factionId)
        }.thenAcceptAsync { it ->
            it ?: return@thenAcceptAsync
            world.execute {
                chunkStore.store.addComponent(chunkRef, FactionChunkComponent.componentType, FactionChunkComponent(
                    chunkIndex = it.chunkIndex,
                    factionId = it.factionId
                ))
                chunkStore.store.getComponent(chunkRef, WorldChunk.getComponentType())!!.markNeedsSaving()
                world.worldMapManager.clearImages()
                world.playerRefs
                    .map { store.getComponent(playerRef.reference!!, Player.getComponentType()) }
                    .forEach { p -> p!!.worldMapTracker.clear() }
                player.sendMessage(successMessage("Chunk claimed."))
            }
        }.exceptionally {
            plugin.logger.atSevere().withCause(it).log("Failed to claim chunk")
            player.sendMessage(errorMessage("Failed to claim chunk: ${it.message}"))
            return@exceptionally null
        }
    }

    override fun hasPermission(sender: CommandSender): Boolean = true
}
