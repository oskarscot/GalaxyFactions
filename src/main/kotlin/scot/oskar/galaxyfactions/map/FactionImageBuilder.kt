package scot.oskar.galaxyfactions.map

import com.hypixel.hytale.math.util.ChunkUtil
import com.hypixel.hytale.protocol.ShaderType
import com.hypixel.hytale.protocol.packets.worldmap.MapImage
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
import com.hypixel.hytale.server.core.universe.world.chunk.palette.BitFieldArr
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import scot.oskar.galaxyfactions.component.FactionChunkComponent
import scot.oskar.galaxyfactions.data.FactionData
import scot.oskar.galaxyfactions.data.FactionId
import scot.oskar.galaxyfactions.data.FactionService
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A lot of this code came from SimpleClaims and have been converted.
 * Original author: https://github.com/Buuz135/SimpleClaims/tree/main
 */
class FactionImageBuilder private constructor(
    val index: Long,
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val world: World,
    private val factionService: FactionService,
) {
    private val rawPixels = IntArray(imageWidth * imageHeight)
    lateinit var image: MapImage
        private set

    private val sampleWidth = min(32, imageWidth)
    private val sampleHeight = min(32, imageHeight)
    private val blockStepX = max(1, 32 / imageWidth)
    private val blockStepZ = max(1, 32 / imageHeight)

    private val heightSamples = ShortArray(sampleWidth * sampleHeight)
    private val tintSamples = IntArray(sampleWidth * sampleHeight)
    private val blockSamples = IntArray(sampleWidth * sampleHeight)
    private val neighborHeightSamples = ShortArray((sampleWidth + 2) * (sampleHeight + 2))
    private val fluidDepthSamples = ShortArray(sampleWidth * sampleHeight)
    private val environmentSamples = IntArray(sampleWidth * sampleHeight)
    private val fluidSamples = IntArray(sampleWidth * sampleHeight)

    private val outColor = Color()
    private lateinit var worldChunk: WorldChunk
    private lateinit var fluidSections: Array<FluidSection?>
    private var factionOverlay: FactionOverlay? = null

    private fun fetchChunk(): CompletableFuture<FactionImageBuilder?> =
        world.chunkStore.getChunkReferenceAsync(index).thenApplyAsync({ ref ->
            if (ref == null || !ref.isValid) return@thenApplyAsync null
            worldChunk = ref.store.getComponent(ref, WorldChunk.getComponentType())!!
            val chunkColumn = ref.store.getComponent(ref, ChunkColumn.getComponentType())!!
            fluidSections = Array(10) { y ->
                world.chunkStore.store.getComponent(chunkColumn.getSection(y)!!, FluidSection.getComponentType())
            }
            factionOverlay = resolveFactionOverlay()
            this
        }, world)

    private fun sampleNeighbors(): CompletableFuture<FactionImageBuilder> {
        val futures = listOf(
            sampleNeighborEdge(worldChunk.x, worldChunk.z - 1) { chunk -> // north
                val z = (sampleHeight - 1) * blockStepZ
                for (ix in 0 until sampleWidth)
                    neighborHeightSamples[1 + ix] = chunk.getHeight(ix * blockStepX, z)
            },
            sampleNeighborEdge(worldChunk.x, worldChunk.z + 1) { chunk -> // south
                val offset = (sampleHeight + 1) * (sampleWidth + 2) + 1
                for (ix in 0 until sampleWidth)
                    neighborHeightSamples[offset + ix] = chunk.getHeight(ix * blockStepX, 0)
            },
            sampleNeighborEdge(worldChunk.x - 1, worldChunk.z) { chunk -> // west
                val x = (sampleWidth - 1) * blockStepX
                for (iz in 0 until sampleHeight)
                    neighborHeightSamples[(iz + 1) * (sampleWidth + 2)] = chunk.getHeight(x, iz * blockStepZ)
            },
            sampleNeighborEdge(worldChunk.x + 1, worldChunk.z) { chunk -> // east
                for (iz in 0 until sampleHeight)
                    neighborHeightSamples[(iz + 1) * (sampleWidth + 2) + sampleWidth + 1] = chunk.getHeight(0, iz * blockStepZ)
            },
            sampleNeighborCorner(worldChunk.x - 1, worldChunk.z - 1, sampleWidth + 1,
                (sampleWidth - 1) * blockStepX, (sampleHeight - 1) * blockStepZ),
            sampleNeighborCorner(worldChunk.x + 1, worldChunk.z - 1, 0,
                0, (sampleHeight - 1) * blockStepZ),
            sampleNeighborCorner(worldChunk.x - 1, worldChunk.z + 1, (sampleHeight + 1) * (sampleWidth + 2),
                (sampleWidth - 1) * blockStepX, 0),
            sampleNeighborCorner(worldChunk.x + 1, worldChunk.z + 1, (sampleHeight + 1) * (sampleWidth + 2) + sampleWidth + 1,
                0, 0),
        )
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply { this }
    }

    private fun sampleNeighborEdge(chunkX: Int, chunkZ: Int, sampler: (WorldChunk) -> Unit): CompletableFuture<Void> =
        world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunkX, chunkZ)).thenAcceptAsync({ ref ->
            if (ref != null && ref.isValid)
                sampler(ref.store.getComponent(ref, WorldChunk.getComponentType())!!)
        }, world)

    private fun sampleNeighborCorner(chunkX: Int, chunkZ: Int, targetIdx: Int, sampleX: Int, sampleZ: Int): CompletableFuture<Void> =
        world.chunkStore.getChunkReferenceAsync(ChunkUtil.indexChunk(chunkX, chunkZ)).thenAcceptAsync({ ref ->
            if (ref != null && ref.isValid)
                neighborHeightSamples[targetIdx] = ref.store.getComponent(ref, WorldChunk.getComponentType())!!.getHeight(sampleX, sampleZ)
        }, world)

    private fun generate(): FactionImageBuilder {
        sampleTerrain()
        copyHeightSamplesToNeighborInterior()
        renderPixels(factionOverlay)
        image = encodeToPalette()
        return this
    }

    private fun sampleTerrain() {
        for (ix in 0 until sampleWidth) {
            for (iz in 0 until sampleHeight) {
                val idx = iz * sampleWidth + ix
                val x = ix * blockStepX
                val z = iz * blockStepZ
                val height = worldChunk.getHeight(x, z)

                heightSamples[idx] = height
                tintSamples[idx] = worldChunk.getTint(x, z)
                blockSamples[idx] = worldChunk.getBlock(x, height.toInt(), z)

                val (fluidId, fluidDepth, envId) = sampleFluid(x, z, height)
                fluidSamples[idx] = fluidId
                fluidDepthSamples[idx] = fluidDepth
                environmentSamples[idx] = envId
            }
        }
    }

    private data class FluidSample(val fluidId: Int, val depth: Short, val environmentId: Int)

    private fun sampleFluid(x: Int, z: Int, height: Short): FluidSample {
        val chunkYGround = ChunkUtil.chunkCoordinate(height.toInt())
        var chunkY = 9
        var fluidId = 0
        var fluidTop = 320
        var fluid: Fluid? = null

        findFluid@ while (chunkY >= 0 && chunkY >= chunkYGround) {
            val section = fluidSections[chunkY]
            if (section != null && !section.isEmpty) {
                for (blockY in ChunkUtil.maxBlock(chunkY) downTo max(ChunkUtil.minBlock(chunkY), height.toInt())) {
                    fluidId = section.getFluidId(x, blockY, z)
                    if (fluidId != 0) {
                        fluid = Fluid.getAssetMap().getAsset(fluidId)
                        fluidTop = blockY
                        break@findFluid
                    }
                }
            }
            chunkY--
        }

        var fluidBottom = height.toInt()
        findBottom@ while (chunkY >= 0 && chunkY >= chunkYGround) {
            val section = fluidSections[chunkY]
            if (section == null || section.isEmpty) {
                fluidBottom = min(ChunkUtil.maxBlock(chunkY) + 1, fluidTop)
                break
            }
            for (blockY in min(ChunkUtil.maxBlock(chunkY), fluidTop - 1) downTo max(ChunkUtil.minBlock(chunkY), height.toInt())) {
                val nextId = section.getFluidId(x, blockY, z)
                if (nextId != fluidId && fluid?.particleColor != Fluid.getAssetMap().getAsset(nextId)?.particleColor) {
                    fluidBottom = blockY + 1
                    break@findBottom
                }
            }
            chunkY--
        }

        val depth: Short = if (fluidId != 0) (fluidTop - fluidBottom + 1).toShort() else 0
        return FluidSample(fluidId, depth, worldChunk.blockChunk!!.getEnvironment(x, fluidTop, z))
    }

    private fun copyHeightSamplesToNeighborInterior() {
        for (iz in 0 until sampleHeight) {
            System.arraycopy(
                heightSamples, iz * sampleWidth,
                neighborHeightSamples, (iz + 1) * (sampleWidth + 2) + 1,
                sampleWidth,
            )
        }
    }

    private data class FactionOverlay(val data: FactionData, val id: UUID, val neighborIds: Array<UUID?>)

    private fun resolveFactionOverlay(): FactionOverlay? {
        val chunkRef = world.chunkStore.getChunkReference(index) ?: return null
        val component = world.chunkStore.store.getComponent(chunkRef, FactionChunkComponent.componentType) ?: return null
        val fId = component.factionId ?: return null
        val data = factionService.getCached(FactionId(fId)) ?: return null
        return FactionOverlay(
            data = data,
            id = fId,
            neighborIds = arrayOf(
                getChunkFactionId(worldChunk.x, worldChunk.z + 1), // south
                getChunkFactionId(worldChunk.x, worldChunk.z - 1), // north
                getChunkFactionId(worldChunk.x + 1, worldChunk.z), // east
                getChunkFactionId(worldChunk.x - 1, worldChunk.z), // west
            ),
        )
    }

    private fun getChunkFactionId(chunkX: Int, chunkZ: Int): UUID? =
        world.chunkStore.getChunkReference(ChunkUtil.indexChunk(chunkX, chunkZ))?.let {
            world.chunkStore.store.getComponent(it, FactionChunkComponent.componentType)?.factionId
        }

    private fun renderPixels(overlay: FactionOverlay?) {
        val ratioW = sampleWidth.toFloat() / imageWidth
        val ratioH = sampleHeight.toFloat() / imageHeight
        val blockPixelW = max(1, imageWidth / sampleWidth)
        val blockPixelH = max(1, imageHeight / sampleHeight)

        for (ix in 0 until imageWidth) {
            for (iz in 0 until imageHeight) {
                val sx = min((ix * ratioW).toInt(), sampleWidth - 1)
                val sz = min((iz * ratioH).toInt(), sampleHeight - 1)
                val idx = sz * sampleWidth + sx

                renderTerrainPixel(ix, iz, sx, sz, idx, blockPixelW, blockPixelH)
                overlay?.let { renderFactionOverlay(ix, iz, it) }
                rawPixels[iz * imageWidth + ix] = outColor.pack()
            }
        }
    }

    private fun renderTerrainPixel(ix: Int, iz: Int, sx: Int, sz: Int, idx: Int, bpw: Int, bph: Int) {
        val height = heightSamples[idx]
        val blockId = blockSamples[idx]

        if (height < 0 || blockId == 0) { outColor.reset(); return }

        getBlockColor(blockId, tintSamples[idx], outColor)
        outColor.multiply(calculateShade(ix, iz, sx, sz, height, bpw, bph))

        if (height < 320) {
            val fluidId = fluidSamples[idx]
            if (fluidId != 0) getFluidColor(fluidId, environmentSamples[idx], fluidDepthSamples[idx], outColor)
        }
    }

    private fun calculateShade(ix: Int, iz: Int, sx: Int, sz: Int, height: Short, bpw: Int, bph: Int): Float {
        val stride = sampleWidth + 2
        return shadeFromHeights(
            ix % bpw, iz % bph, bpw, bph, height,
            neighborHeightSamples[sz * stride + sx + 1],
            neighborHeightSamples[(sz + 2) * stride + sx + 1],
            neighborHeightSamples[(sz + 1) * stride + sx],
            neighborHeightSamples[(sz + 1) * stride + sx + 2],
            neighborHeightSamples[sz * stride + sx],
            neighborHeightSamples[sz * stride + sx + 2],
            neighborHeightSamples[(sz + 2) * stride + sx],
            neighborHeightSamples[(sz + 2) * stride + sx + 2],
        )
    }

    private fun renderFactionOverlay(ix: Int, iz: Int, overlay: FactionOverlay) {
        val borderSize = 2
        val isBorder =
            (ix <= borderSize && overlay.neighborIds[3] != overlay.id) ||
            (ix >= imageWidth - borderSize - 1 && overlay.neighborIds[2] != overlay.id) ||
            (iz <= borderSize && overlay.neighborIds[1] != overlay.id) ||
            (iz >= imageHeight - borderSize - 1 && overlay.neighborIds[0] != overlay.id)
        applyFactionOverlay(overlay.data.color, outColor, isBorder)
    }

    private fun encodeToPalette(): MapImage {
        val pixelCount = rawPixels.size
        val processed = IntArray(pixelCount)
        val uniqueColors = IntOpenHashSet()

        for (i in 0 until pixelCount) {
            processed[i] = if (QUANTIZATION_ENABLED) {
                if (isInTransitionZone(i)) quantizeColorWithDither(rawPixels[i], i % imageWidth, i / imageWidth)
                else quantizeColor(rawPixels[i])
            } else rawPixels[i]
            uniqueColors.add(processed[i])
        }

        val palette = uniqueColors.toIntArray()
        val bitsPerIndex = calculateBitsRequired(palette.size)
        val colorToIndex = Int2IntOpenHashMap(palette.size).apply {
            palette.forEachIndexed { i, color -> put(color, i) }
        }
        val indices = BitFieldArr(bitsPerIndex, pixelCount).apply {
            for (i in 0 until pixelCount) set(i, colorToIndex.get(processed[i]))
        }
        return MapImage(imageWidth, imageHeight, palette, bitsPerIndex.toByte(), indices.get())
    }

    private fun isInTransitionZone(index: Int): Boolean {
        val center = quantizeColor(rawPixels[index])
        val x = index % imageWidth
        val y = index / imageWidth
        for (dy in -2..2) for (dx in -2..2) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until imageWidth && ny in 0 until imageHeight &&
                quantizeColor(rawPixels[ny * imageWidth + nx]) != center) return true
        }
        return false
    }

    private class Color {
        var r = 0; var g = 0; var b = 0; var a = 0
        fun pack() = (r and 0xFF shl 24) or (g and 0xFF shl 16) or (b and 0xFF shl 8) or (a and 0xFF)
        fun reset() { r = 0; g = 0; b = 0; a = 0 }
        fun multiply(value: Float) {
            r = (r * value).toInt().coerceIn(0, 255)
            g = (g * value).toInt().coerceIn(0, 255)
            b = (b * value).toInt().coerceIn(0, 255)
        }
    }

    companion object {
        private const val QUANTIZATION_ENABLED = true
        private const val BORDER_ALPHA = 0.75f
        private const val INTERIOR_ALPHA = 0.4f
        private val BAYER_MATRIX = arrayOf(
            intArrayOf(0, 8, 2, 10), intArrayOf(12, 4, 14, 6),
            intArrayOf(3, 11, 1, 9), intArrayOf(15, 7, 13, 5),
        )

        fun build(index: Long, imageWidth: Int, imageHeight: Int, world: World, factionService: FactionService): CompletableFuture<FactionImageBuilder?> =
            CompletableFuture.completedFuture(FactionImageBuilder(index, imageWidth, imageHeight, world, factionService))
                .thenCompose { it.fetchChunk() }
                .thenCompose { it?.sampleNeighbors() ?: CompletableFuture.completedFuture(null) }
                .thenApplyAsync { it?.generate() }

        private fun quantizeChannel(value: Int) = min(255, (value + 4) / 8 * 8)

        private fun quantizeChannelWithDither(value: Int, offset: Int) =
            min(255, ((value + offset).coerceIn(0, 255) + 4) / 8 * 8)

        private fun quantizeColor(argb: Int): Int {
            val r = quantizeChannel(argb shr 24 and 0xFF)
            val g = quantizeChannel(argb shr 16 and 0xFF)
            val b = quantizeChannel(argb shr 8 and 0xFF)
            return (r shl 24) or (g shl 16) or (b shl 8) or (argb and 0xFF)
        }

        private fun quantizeColorWithDither(argb: Int, x: Int, y: Int): Int {
            val offset = (BAYER_MATRIX[y and 3][x and 3] - 8) * 8 / 16
            val r = quantizeChannelWithDither(argb shr 24 and 0xFF, offset)
            val g = quantizeChannelWithDither(argb shr 16 and 0xFF, offset)
            val b = quantizeChannelWithDither(argb shr 8 and 0xFF, offset)
            return (r shl 24) or (g shl 16) or (b shl 8) or (argb and 0xFF)
        }

        private fun calculateBitsRequired(colorCount: Int) = when {
            colorCount <= 16 -> 4; colorCount <= 256 -> 8; colorCount <= 4096 -> 12; else -> 16
        }

        private fun shadeFromHeights(
            bpx: Int, bpz: Int, bpw: Int, bph: Int,
            h: Short, n: Short, s: Short, w: Short, e: Short,
            nw: Short, ne: Short, sw: Short, se: Short,
        ): Float {
            val u = (bpx + 0.5f) / bpw
            val v = (bpz + 0.5f) / bph
            val dhdx = ((h - w) * (1 - u) + (e - h) * u) * 2 + (h - nw) * (1 - (u + v) / 2) + (se - h) * ((u + v) / 2)
            val dhdz = ((h - n) * (1 - v) + (s - h) * v) * 2 + (h - ne) * (1 - (1 - u + v) / 2) + (sw - h) * ((1 - u + v) / 2)
            val dy = 3.0f
            val invS = 1.0f / sqrt(dhdx * dhdx + dy * dy + dhdz * dhdz)
            var lx = -0.2f; var ly = 0.8f; var lz = 0.5f
            val invL = 1.0f / sqrt(lx * lx + ly * ly + lz * lz)
            lx *= invL; ly *= invL; lz *= invL
            return 0.4f + 0.6f * max(0.0f, dhdx * invS * lx + dy * invS * ly + dhdz * invS * lz)
        }

        private fun getBlockColor(blockId: Int, biomeTint: Int, out: Color) {
            val block = BlockType.getAssetMap().getAsset(blockId)!!
            val bR = biomeTint shr 16 and 0xFF
            val bG = biomeTint shr 8 and 0xFF
            val bB = biomeTint and 0xFF
            val tintUp = block.tintUp
            val hasTint = !tintUp.isNullOrEmpty()
            val sR = if (hasTint) tintUp[0].red.toInt() and 0xFF else 255
            val sG = if (hasTint) tintUp[0].green.toInt() and 0xFF else 255
            val sB = if (hasTint) tintUp[0].blue.toInt() and 0xFF else 255
            val f = block.biomeTintUp / 100.0f
            var r = (sR + (bR - sR) * f).toInt()
            var g = (sG + (bG - sG) * f).toInt()
            var b = (sB + (bB - sB) * f).toInt()
            block.particleColor?.takeIf { f < 1.0f }?.let {
                r = r * (it.red.toInt() and 0xFF) / 255
                g = g * (it.green.toInt() and 0xFF) / 255
                b = b * (it.blue.toInt() and 0xFF) / 255
            }
            out.r = r and 0xFF; out.g = g and 0xFF; out.b = b and 0xFF; out.a = 255
        }

        private fun getFluidColor(fluidId: Int, envId: Int, depth: Short, out: Color) {
            val fluid = Fluid.getAssetMap().getAsset(fluidId) ?: return
            var r = 255; var g = 255; var b = 255
            if (fluid.hasEffect(ShaderType.Water)) {
                Environment.getAssetMap().getAsset(envId)!!.waterTint?.let {
                    r = r * (it.red.toInt() and 0xFF) / 255
                    g = g * (it.green.toInt() and 0xFF) / 255
                    b = b * (it.blue.toInt() and 0xFF) / 255
                }
            }
            fluid.particleColor?.let {
                r = r * (it.red.toInt() and 0xFF) / 255
                g = g * (it.green.toInt() and 0xFF) / 255
                b = b * (it.blue.toInt() and 0xFF) / 255
            }
            val f = min(1.0f, 1.0f / depth)
            out.r = (r + ((out.r and 0xFF) - r) * f).toInt() and 0xFF
            out.g = (g + ((out.g and 0xFF) - g) * f).toInt() and 0xFF
            out.b = (b + ((out.b and 0xFF) - b) * f).toInt() and 0xFF
        }

        private fun applyFactionOverlay(color: Int, out: Color, isBorder: Boolean) {
            val alpha = if (isBorder) BORDER_ALPHA else INTERIOR_ALPHA
            val inv = 1 - alpha
            out.r = (out.r * inv + (color shr 16 and 0xFF) * alpha).toInt()
            out.g = (out.g * inv + (color shr 8 and 0xFF) * alpha).toInt()
            out.b = (out.b * inv + (color and 0xFF) * alpha).toInt()
            out.a = 255
        }
    }
}
