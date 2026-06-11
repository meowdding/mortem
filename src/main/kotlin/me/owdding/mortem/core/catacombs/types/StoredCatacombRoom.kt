package me.owdding.mortem.core.catacombs.types

import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec

@GenerateCodec
data class StoredCatacombRoom(
    val name: String,
    val hashes: List<Int>,
    @FieldName("room_type") val roomType: CatacombRoomType?,
    @FieldName("secret_count") val secretCount: Int = 0,
    @FieldName("dungeon_room_mod_id") val dungeonRoomModId: String?,
    @FieldName("puzzle_type") val puzzleType: CatacombPuzzleType?,
) {
    var shouldSerialize = false

    fun markChange() {
        shouldSerialize = true
    }
}
