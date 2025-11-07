package me.owdding.mortem.utils.extensions

import me.owdding.mortem.mixins.ClientboundSectionBlocksUpdatePacketAccessor
import net.minecraft.core.SectionPos
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket

val ClientboundSectionBlocksUpdatePacket.sectionPos: SectionPos get() = (this as ClientboundSectionBlocksUpdatePacketAccessor).`mortem$sectionPos`()
