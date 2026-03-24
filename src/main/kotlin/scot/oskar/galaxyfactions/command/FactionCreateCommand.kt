package scot.oskar.galaxyfactions.command

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.CommandSender
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import kotlinx.coroutines.launch
import scot.oskar.galaxyfactions.FactionsPlugin
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.data.ChunkIndexId
import scot.oskar.galaxyfactions.data.FactionId
import scot.oskar.galaxyfactions.ext.futureFromSuspend

class FactionCreateCommand(private val plugin: FactionsPlugin) : AbstractPlayerCommand("createfaction", "Create a faction") {

    private val nameArg: RequiredArg<String> = this.withRequiredArg("name", "Name of the faction", ArgTypes.STRING)

    override fun execute(
        context: CommandContext,
        store: Store<EntityStore>,
        ref: Ref<EntityStore>,
        playerRef: PlayerRef,
        world: World
    ) {
        val name = nameArg.get(context)
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        val playerTransform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
        val chunkIndex = ChunkUtil.indexChunkFromBlock(playerTransform.position.x.toInt(), playerTransform.position.z.toInt())
        val chunkStore = world.chunkStore
        val chunkReference = chunkStore.getChunkReference(chunkIndex)!! // impossible to be null
        val existing = chunkStore.store.getComponent(chunkReference, FactionChunkComponent.componentType)
        if (existing != null) {
            player.sendMessage(Message.raw("This chunk is already claimed by another faction."))
            return
        }

        plugin.scope.futureFromSuspend {
            val existingFaction = plugin.factionRepository.findByName(name)
            if (existingFaction != null) {
                player.sendMessage(Message.raw("Faction '$name' already exists."))
                return@futureFromSuspend null
            }
            val faction = plugin.factionService.createFaction(playerRef.uuid, name)
            player.sendMessage(Message.raw("Created faction ${faction.name}"))
            plugin.factionService.createChunkForFaction(ChunkIndexId(chunkIndex), FactionId(faction.id))
        }.thenAcceptAsync {
            it ?: return@thenAcceptAsync
            world.execute {
                val factionChunkComponent = FactionChunkComponent(
                    chunkIndex = it.chunkIndex,
                    factionId = it.factionId
                )
                chunkStore.store.addComponent(chunkReference, FactionChunkComponent.componentType, factionChunkComponent)
            }
        }.exceptionally {
            plugin.logger.atSevere().withCause(it).log("Failed to create faction")
            player.sendMessage(Message.raw("Could not create faction: ${it.message}"))
            return@exceptionally null
        }
    }

    override fun hasPermission(sender: CommandSender): Boolean = true
}
