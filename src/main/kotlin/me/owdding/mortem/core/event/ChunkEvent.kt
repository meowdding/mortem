package me.owdding.mortem.core.event

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.chunk.LevelChunk
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

sealed class ChunkEvent(
    val level: ClientLevel,
    val chunk: LevelChunk
) : SkyBlockEvent() {
    class ChunkLoadEvent(
        level: ClientLevel,
        chunk: LevelChunk
    ) : ChunkEvent(level, chunk)

    class ChunkUnloadEvent(
        level: ClientLevel,
        chunk: LevelChunk
    ) : ChunkEvent(level, chunk)

    class ChunkUpdateEvent(
        level: ClientLevel,
        chunk: LevelChunk,
    ) : ChunkEvent(level, chunk)
}
