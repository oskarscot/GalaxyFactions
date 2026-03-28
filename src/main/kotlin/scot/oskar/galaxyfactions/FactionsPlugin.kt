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
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import scot.oskar.galaxyfactions.command.FactionClaimCommand
import scot.oskar.galaxyfactions.command.FactionDeleteCommand
import scot.oskar.galaxyfactions.command.FactionUnclaimCommand
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.component.FactionChunkComponentCodec
import scot.oskar.galaxyfactions.config.FactionsPluginConfigCodec
import scot.oskar.galaxyfactions.data.FactionChunkRepository
import scot.oskar.galaxyfactions.data.FactionChunks
import scot.oskar.galaxyfactions.data.FactionMemberRepository
import scot.oskar.galaxyfactions.data.FactionMembers
import scot.oskar.galaxyfactions.data.FactionRepository
import scot.oskar.galaxyfactions.data.Factions
import com.hypixel.hytale.codec.lookup.Priority
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider
import scot.oskar.galaxyfactions.data.FactionService
import scot.oskar.galaxyfactions.map.FactionWorldMap
import scot.oskar.galaxyfactions.map.FactionWorldMapProvider
import scot.oskar.galaxyfactions.system.ChunkChangeEventSystem
import scot.oskar.galaxyfactions.system.PlayerMovementSystem

class FactionsPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val config: Config<FactionsPluginConfig>
    lateinit var database: Database
    lateinit var factionRepository: FactionRepository
    lateinit var factionChunkRepository: FactionChunkRepository
    lateinit var factionService: FactionService
    lateinit var factionMemberRepository: FactionMemberRepository

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
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(Factions, FactionMembers, FactionChunks)
        }

        FactionChunkComponent.componentType = chunkStoreRegistry.registerComponent(FactionChunkComponent::class.java, "FactionChunk", FactionChunkComponentCodec)

        factionRepository = FactionRepository(database)
        factionChunkRepository = FactionChunkRepository(database)
        factionService = FactionService(factionRepository, factionChunkRepository)
        factionMemberRepository = FactionMemberRepository(database)

        scope.launch { factionService.loadAll() }

        IWorldMapProvider.CODEC.register(
            Priority.DEFAULT.before(),
            FactionWorldMapProvider.ID,
            FactionWorldMapProvider::class.java,
            FactionWorldMapProvider.createCodec(factionService)
        )

        logger.atInfo().log("FactionsPlugin!!!")
        commandRegistry.registerCommand(TestCommand())
        commandRegistry.registerCommand(FactionCreateCommand(this))
        commandRegistry.registerCommand(FactionDeleteCommand(this))
        commandRegistry.registerCommand(FactionClaimCommand(this))
        commandRegistry.registerCommand(FactionUnclaimCommand(this))
    }

    override fun start() {
        entityStoreRegistry.registerSystem(PlayerMovementSystem())
        entityStoreRegistry.registerSystem(ChunkChangeEventSystem(factionService))
    }

    override fun shutdown() {
        scope.cancel()
    }
}