package me.owdding.mortem.core.catacombs.types

enum class MapSize(
    val roomWidth: Int,
    val offset: Int,
) {
    SMALL(22, 0),
    LARGE(20, 1)
}
