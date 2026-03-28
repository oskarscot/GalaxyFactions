package scot.oskar.galaxyfactions.map

import com.hypixel.hytale.protocol.packets.worldmap.MapMarker
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.map.WorldMap
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.chunk.ChunkWorldMap
import it.unimi.dsi.fastutil.longs.LongSet
import scot.oskar.galaxyfactions.data.FactionService
import java.util.Collections
import java.util.concurrent.CompletableFuture

class FactionWorldMap(
    private val factionService: FactionService,
) : IWorldMap {

    override fun getWorldMapSettings(): WorldMapSettings = ChunkWorldMap.INSTANCE.worldMapSettings

    override fun generate(world: World, imageWidth: Int, imageHeight: Int, chunksToGenerate: LongSet): CompletableFuture<WorldMap> {
        val iter = chunksToGenerate.iterator()
        val futures = Array(chunksToGenerate.size) {
            FactionImageBuilder.build(iter.nextLong(), imageWidth, imageHeight, world, factionService)
        }

        return CompletableFuture.allOf(*futures).thenApply {
            val worldMap = WorldMap(futures.size)
            for (future in futures) {
                val builder = future.getNow(null) ?: continue
                worldMap.chunks.put(builder.index, builder.image)
            }
            worldMap
        }
    }

    override fun generatePointsOfInterest(world: World): CompletableFuture<Map<String, MapMarker>> {
        return CompletableFuture.completedFuture(Collections.emptyMap())
    }
}
