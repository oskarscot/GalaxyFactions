package scot.oskar.galaxyfactions.map

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider
import scot.oskar.galaxyfactions.data.FactionService

class FactionWorldMapProvider(
    private val factionService: FactionService,
) : IWorldMapProvider {

    override fun getGenerator(world: World): IWorldMap = FactionWorldMap(factionService)

    companion object {
        const val ID = "GalaxyFactions"

        fun createCodec(factionService: FactionService): BuilderCodec<FactionWorldMapProvider> =
            BuilderCodec.builder(FactionWorldMapProvider::class.java) { FactionWorldMapProvider(factionService) }.build()
    }
}
