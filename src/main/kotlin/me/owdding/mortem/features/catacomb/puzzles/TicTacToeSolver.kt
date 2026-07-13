package me.owdding.mortem.features.catacomb.puzzles

import me.owdding.mortem.core.catacombs.types.CatacombPuzzleType

object TicTacToeSolver : AbstractPuzzleSolver() {
    enum class TileState {
        X,
        O,
        F,
        ;
    }

    override val puzzleFilter: (CatacombPuzzleType?) -> Boolean = { it == CatacombPuzzleType.TIC_TAC_TOE || it == CatacombPuzzleType.TIC_TAC_TOE_NEW }

    fun analyzePuzzle() = if (isCurrentlyInPuzzle) {

    } else Unit

    override fun reset() = analyzePuzzle()
    override fun enterRoom() = analyzePuzzle()
    override fun leaveRoom() {

    }
}
