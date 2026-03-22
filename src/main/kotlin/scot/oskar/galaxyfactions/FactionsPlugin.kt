package scot.oskar.galaxyfactions

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import scot.oskar.galaxyfactions.command.TestCommand
import scot.oskar.galaxyfactions.config.FactionsPluginConfig
import com.hypixel.hytale.server.core.util.Config
import org.jetbrains.exposed.v1.jdbc.Database
import scot.oskar.galaxyfactions.config.FactionsPluginConfigCodec
import scot.oskar.galaxyfactions.data.FactionChunkRepository
import scot.oskar.galaxyfactions.data.FactionRepository
import scot.oskar.galaxyfactions.event.ChunkChangeEvent
import scot.oskar.galaxyfactions.event.PlayerMoveEvent
import scot.oskar.galaxyfactions.system.PlayerMovementSystem

class FactionsPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    val config: Config<FactionsPluginConfig> = withConfig(FactionsPluginConfigCodec)
    val database: Database
    val factionRepository: FactionRepository
    val factionChunkRepository: FactionChunkRepository

    init {
        database = Database.connect(
            url = config.get().jdbcUrl,
            user = config.get().username,
            password = config.get().password,
            driver = "org.postgresql.Driver"
        )
        factionRepository = FactionRepository(database)
        factionChunkRepository = FactionChunkRepository(database)
    }

    override fun setup() {
        logger.atInfo().log("FactionsPlugin!!!")
        commandRegistry.registerCommand(TestCommand())
    }

    override fun start() {
        entityStoreRegistry.registerSystem(PlayerMovementSystem())
    }
}