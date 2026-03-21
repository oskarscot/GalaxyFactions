package scot.oskar.galaxyfactions

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import scot.oskar.galaxyfactions.command.DumpChunkCommand
import scot.oskar.galaxyfactions.command.TestCommand
import scot.oskar.galaxyfactions.event.ChunkChangeEvent
import scot.oskar.galaxyfactions.event.PlayerMoveEvent
import scot.oskar.galaxyfactions.listener.onChunkChange
import scot.oskar.galaxyfactions.listener.onPlayerMove
import scot.oskar.galaxyfactions.system.PlayerMovementSystem

class FactionsPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    override fun setup() {
        logger.atInfo().log("FactionsPlugin!!!")
        commandRegistry.registerCommand(TestCommand())
        commandRegistry.registerCommand(DumpChunkCommand())
    }

    override fun start() {
        entityStoreRegistry.registerSystem(PlayerMovementSystem())
        eventRegistry.registerGlobal(PlayerMoveEvent::class.java, ::onPlayerMove)
        eventRegistry.registerGlobal(ChunkChangeEvent::class.java, ::onChunkChange)
    }
}