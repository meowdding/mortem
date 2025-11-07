package me.owdding.mortem.utils.tag

import me.owdding.mortem.Mortem
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import tech.thatgravyboat.skyblockapi.impl.tagkey.BlockTagKey

enum class BlockTagKey(path: String) : BlockTagKey {
    IGNORED_BLOCKS("ignored_blocks"),
    ;

    operator fun contains(element: BlockState): Boolean = element.block in this
    override val key: TagKey<Block> = TagKey.create(Registries.BLOCK, Mortem.id(path))
}
