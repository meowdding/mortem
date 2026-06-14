package me.owdding.mortem.core.catacombs.types

enum class CatacombRating(
    val minScore: Int,
    val shortName: String,
) {
    D(0, "D"),
    C(100, "C"),
    B(160, "B"),
    A(230, "A"),
    S(270, "S"),
    SPLUS(300, "S+"),
    ;

    companion object {
        fun getByScore(score: Int?): CatacombRating? {
            if (score != null) {
                return entries.lastOrNull { score >= it.minScore }
            }
            return null
        }
    }
}
