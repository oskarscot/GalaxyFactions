package scot.oskar.galaxyfactions.command

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import scot.oskar.galaxyfactions.FactionsPlugin
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.data.FactionId
import scot.oskar.galaxyfactions.ext.errorMessage
import scot.oskar.galaxyfactions.ext.futureFromSuspend
import scot.oskar.galaxyfactions.ext.successMessage

class FactionDeleteCommand(private val plugin: FactionsPlugin): AbstractPlayerCommand("factiondelete", "Deletes a faction") {

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        plugin.scope.futureFromSuspend {
            val factionData = plugin.factionRepository.findByMember(playerRef.uuid)
            if(factionData == null) {
                playerRef.sendMessage(errorMessage("You're not in a faction."))
                return@futureFromSuspend null
            }
            if(factionData.owner != playerRef.uuid) {
                playerRef.sendMessage(errorMessage("You're not the owner of the faction."))
                return@futureFromSuspend null
            }
            val factionId = FactionId(factionData.id)
            val chunks = plugin.factionChunkRepository.findByFactionId(factionId)
            plugin.factionRepository.deleteById(factionId)
            plugin.factionService.removeCached(factionId)
            playerRef.sendMessage(successMessage("Faction '${factionData.name}' has been deleted."))
            chunks
        }.thenAcceptAsync { chunks ->
            chunks ?: return@thenAcceptAsync
            world.execute {
                val chunkStore = world.chunkStore
                for (chunk in chunks) {
                    val chunkRef = chunkStore.getChunkReference(chunk.chunkIndex) ?: continue
                    val existing = chunkStore.store.getComponent(chunkRef, FactionChunkComponent.componentType) ?: continue
                    chunkStore.store.removeComponent(chunkRef, FactionChunkComponent.componentType)
                    chunkStore.store.getComponent(chunkRef, WorldChunk.getComponentType())!!.markNeedsSaving()
                }
                world.worldMapManager.clearImages()
                world.playerRefs
                    .map { store.getComponent(playerRef.reference!!, Player.getComponentType()) }
                    .forEach { player -> player!!.worldMapTracker.clear() }
            }
        }.exceptionally {
            playerRef.sendMessage(errorMessage("Failed to delete the faction."))
            plugin.logger.atSevere().withCause(it).log("Failed to delete faction for player ${playerRef.uuid}")
            return@exceptionally null
        }
    }

    override fun hasPermission(sender: CommandSender): Boolean = true

}