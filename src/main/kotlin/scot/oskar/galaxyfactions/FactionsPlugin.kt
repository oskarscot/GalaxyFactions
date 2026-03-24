package scot.oskar.galaxyfactions

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import scot.oskar.galaxyfactions.command.FactionCreateCommand
import scot.oskar.galaxyfactions.command.TestCommand
import scot.oskar.galaxyfactions.config.FactionsPluginConfig
import com.hypixel.hytale.server.core.util.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.exposed.v1.jdbc.Database
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.component.FactionChunkComponentCodec
import scot.oskar.galaxyfactions.config.FactionsPluginConfigCodec
import scot.oskar.galaxyfactions.data.FactionChunkRepository
import scot.oskar.galaxyfactions.data.FactionRepository
import scot.oskar.galaxyfactions.data.FactionService
import scot.oskar.galaxyfactions.system.FactionChunkSystem
import scot.oskar.galaxyfactions.system.PlayerMovementSystem

class FactionsPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val config: Config<FactionsPluginConfig>
    lateinit var database: Database
    lateinit var factionRepository: FactionRepository
    lateinit var factionChunkRepository: FactionChunkRepository
    lateinit var factionService: FactionService

    init {
        config = withConfig(FactionsPluginConfigCodec)
    }

    override fun setup() {
        database = Database.connect(
            url = config.get().jdbcUrl,
            user = config.get().username,
            password = config.get().password,
            driver = "org.postgresql.Driver"
        )
        FactionChunkComponent.componentType = chunkStoreRegistry.registerComponent(FactionChunkComponent::class.java, "FactionChunk", FactionChunkComponentCodec)

        factionRepository = FactionRepository(database)
        factionChunkRepository = FactionChunkRepository(database)
        factionService = FactionService(factionRepository, factionChunkRepository)

        logger.atInfo().log("FactionsPlugin!!!")
        commandRegistry.registerCommand(TestCommand())
        commandRegistry.registerCommand(FactionCreateCommand(this))
    }

    override fun start() {
        entityStoreRegistry.registerSystem(PlayerMovementSystem())
        chunkStoreRegistry.registerSystem(FactionChunkSystem())
    }

    override fun shutdown() {
        scope.cancel()
    }
}