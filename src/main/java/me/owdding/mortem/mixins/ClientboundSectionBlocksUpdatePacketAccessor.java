package me.owdding.mortem.mixins;

import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public interface ClientboundSectionBlocksUpdatePacketAccessor {

    @Accessor("sectionPos")
    SectionPos mortem$sectionPos();

}
