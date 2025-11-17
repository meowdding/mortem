package me.owdding.mortem.utils.import_data

import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.FieldNames
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.mortem.core.catacombs.CatacombRoomType
import me.owdding.mortem.core.catacombs.SecretDetails
import me.owdding.mortem.core.catacombs.nodes.CatacombRoomShape

@GenerateCodec
data class RoomData(
    val id: List<String>,
    val name: String,
    val type: RoomType,
    val shape: Shape,
    val secrets: Int,
    val crypts: Int,
    val journals: Int,
    val spiders: Boolean = false,
    @FieldName("secret_details") val secretDetails: SecretDetails?,
    @FieldNames("fairy_soul", "soul") val fairySoul: Boolean
)

enum class RoomType(val type: CatacombRoomType) {
    BLOOD(CatacombRoomType.PUZZLE),
    FAIRY(CatacombRoomType.FAIRY),
    GOLD(CatacombRoomType.MINIBOSS),
    MINIBOSS(CatacombRoomType.NORMAL),
    MOBS(CatacombRoomType.NORMAL),
    PUZZLE(CatacombRoomType.PUZZLE),
    RARE(CatacombRoomType.NORMAL),
    SPAWN(CatacombRoomType.START),
    TRAP(CatacombRoomType.TRAP),
    ;
}

enum class Shape(val catacombShape: CatacombRoomShape) {
    L(CatacombRoomShape.STAIR),
    `1x1`(CatacombRoomShape.ONE_BY_ONE),
    `1x2`(CatacombRoomShape.ONE_BY_TWO),
    `1x3`(CatacombRoomShape.ONE_BY_THREE),
    `1x4`(CatacombRoomShape.ONE_BY_FOUR),
    `2x2`(CatacombRoomShape.TWO_BY_TWO),
}
