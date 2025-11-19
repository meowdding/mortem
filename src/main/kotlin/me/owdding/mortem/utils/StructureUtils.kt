package me.owdding.mortem.utils

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.level.storage.TagValueOutput
import tech.thatgravyboat.skyblockapi.utils.extentions.minus
import tech.thatgravyboat.skyblockapi.utils.extentions.plus
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min

object StructureUtils {
    // mostest efficientest code ever written
    fun encodeStructureFromRegions(
        level: Level,
        regions: Iterable<Pair<BlockPos, BlockPos>>,
        rotation: Rotation = Rotation.NONE,
        ignored: List<Block> = listOf(Blocks.AIR, Blocks.VOID_AIR, Blocks.CAVE_AIR, Blocks.STRUCTURE_VOID),
    ): CompoundTag? {

        var minCorner: BlockPos? = null
        var maxCorner: BlockPos? = null

        val queuedBlocks = mutableMapOf<BlockPos, BlockState>()

        for (region in regions) {
            val regionMin = BlockPos(
                min(region.first.x, region.second.x),
                min(region.first.y, region.second.y),
                min(region.first.z, region.second.z),
            )
            val regionMax = BlockPos(
                max(region.first.x, region.second.x),
                max(region.first.y, region.second.y),
                max(region.first.z, region.second.z),
            )

            if (!level.isLoaded(regionMin) || !level.isLoaded(regionMax)) return null

            var blockMin = regionMax
            var blockMax = regionMin

            for (blockPos in BlockPos.betweenClosed(regionMin, regionMax)) {
                if (blockPos in queuedBlocks) continue

                val blockState = level.getBlockState(blockPos)
                if (!ignored.stream().anyMatch(blockState::`is`)) {
                    if (blockMax.x > blockPos.x || blockMax.y > blockPos.y || blockMax.z > blockPos.z) {
                        blockMin = BlockPos(
                            min(blockPos.x, blockMin.x),
                            min(blockPos.y, blockMin.y),
                            min(blockPos.z, blockMin.z),
                        )
                    }
                    if (blockMax.x < blockPos.x || blockMax.y < blockPos.y || blockMax.z < blockPos.z) {
                        blockMax = BlockPos(
                            max(blockPos.x, blockMax.x),
                            max(blockPos.y, blockMax.y),
                            max(blockPos.z, blockMax.z),
                        )
                    }
                    queuedBlocks[blockPos.immutable()] = blockState
                }
            }

            if (blockMin == regionMax) blockMin = regionMin
            if (blockMax == regionMin) blockMax = regionMax

            if (minCorner == null || maxCorner == null) {
                minCorner = blockMin
                maxCorner = blockMax
            } else {
                minCorner = BlockPos(
                    min(blockMin.x, minCorner.x),
                    min(blockMin.y, minCorner.y),
                    min(blockMin.z, minCorner.z),
                )
                maxCorner = BlockPos(
                    max(blockMax.x, maxCorner.x),
                    max(blockMax.y, maxCorner.y),
                    max(blockMax.z, maxCorner.z),
                )
            }
        }

        if (minCorner == null || maxCorner == null) return null

        val size = maxCorner - minCorner + BlockPos(1, 1, 1)

        val rotatedSize = when (rotation) {
            Rotation.NONE, Rotation.CLOCKWISE_180 -> size
            Rotation.CLOCKWISE_90, Rotation.COUNTERCLOCKWISE_90 -> BlockPos(size.z, size.y, size.x)
        }

        val template = StructureTemplate()
        val fullBlockList = mutableListOf<StructureTemplate.StructureBlockInfo>()
        val blockEntitiesList = mutableListOf<StructureTemplate.StructureBlockInfo>()
        val otherBlocksList = mutableListOf<StructureTemplate.StructureBlockInfo>()

        val structureEntities = mutableListOf<StructureTemplate.StructureEntityInfo>()

        for ((blockPos, blockState) in queuedBlocks) {
            if (isOutOfBounds(minCorner, maxCorner, blockPos)) continue
            if (ignored.stream().anyMatch(blockState::`is`)) continue

            val relativeBlockPos = blockPos.subtract(minCorner)

            val rotatedBlockPos = when (rotation) {
                Rotation.NONE -> relativeBlockPos
                Rotation.CLOCKWISE_90 -> BlockPos(
                    rotatedSize.x - relativeBlockPos.z - 1,
                    relativeBlockPos.y,
                    relativeBlockPos.x,
                )

                Rotation.CLOCKWISE_180 -> BlockPos(
                    rotatedSize.x - relativeBlockPos.x - 1,
                    relativeBlockPos.y,
                    rotatedSize.z - relativeBlockPos.z - 1,
                )

                Rotation.COUNTERCLOCKWISE_90 -> BlockPos(
                    relativeBlockPos.z,
                    relativeBlockPos.y,
                    rotatedSize.z - relativeBlockPos.x - 1,
                )
            }

            val rotatedBlockState = blockState.rotate(rotation)

            val blockEntity = level.getBlockEntity(blockPos)

            val info = if (blockEntity != null) {
                val tagValueOutput =
                    TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess())
                blockEntity.saveWithId(tagValueOutput)
                StructureTemplate.StructureBlockInfo(rotatedBlockPos, rotatedBlockState, tagValueOutput.buildResult())
            } else {
                StructureTemplate.StructureBlockInfo(rotatedBlockPos, rotatedBlockState, null)
            }

            when {
                info.nbt != null -> blockEntitiesList.add(info)
                info.state.block.hasDynamicShape() || !info.state.isCollisionShapeFullBlock(
                    EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO,
                ) -> otherBlocksList.add(info)

                else -> fullBlockList.add(info)
            }
        }

        val blockInfoList = buildList {
            val comparator = Comparator.comparingInt<StructureTemplate.StructureBlockInfo> { it.pos.y }
                .thenComparingInt { it.pos.x }
                .thenComparingInt { it.pos.z }

            fullBlockList.sortWith(comparator)
            addAll(fullBlockList)
            otherBlocksList.sortWith(comparator)
            addAll(otherBlocksList)
            blockEntitiesList.sortWith(comparator)
            addAll(blockEntitiesList)
        }

        template.palettes.clear()
        template.palettes.add(StructureTemplate.Palette(blockInfoList))
        template.entityInfoList.addAll(structureEntities)

        template.size = rotatedSize

        val nbt = CompoundTag()

        template.save(nbt)

        val blockPosCodec = BlockPos.CODEC

        val encodedMinPos = blockPosCodec.encodeStart(NbtOps.INSTANCE, minCorner).getOrThrow()
        val encodedMaxPos = blockPosCodec.encodeStart(NbtOps.INSTANCE, maxCorner).getOrThrow()

        nbt.putString("source_rotation", rotation.serializedName)
        nbt.put("source_min_corner", encodedMinPos)
        nbt.put("source_max_corner", encodedMaxPos)
        nbt.putLong("export_time", System.currentTimeMillis())

        return nbt
    }

    private fun isOutOfBounds(startPos: BlockPos, endPos: BlockPos, query: BlockPos) = query.x > endPos.x || query.x < startPos.x ||
        query.y > endPos.y || query.y < startPos.y ||
        query.z > endPos.z || query.z < startPos.z
}
