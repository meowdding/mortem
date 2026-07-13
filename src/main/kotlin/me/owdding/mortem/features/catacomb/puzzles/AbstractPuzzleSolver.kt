package me.owdding.mortem.features.catacomb.puzzles

import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.types.CatacombPuzzleType
import me.owdding.mortem.core.catacombs.types.CatacombRoomCheckmark
import me.owdding.mortem.core.event.catacomb.CatacombRoomChangeEvent
import me.owdding.mortem.core.event.catacomb.CatacombRoomCheckmarkChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.chat.ChatReceivedEvent
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.match

abstract class AbstractPuzzleSolver {
    var isCurrentlyInPuzzle = false
        set(value) {
            if (value == field) return
            if (value) {
                enterRoom()
            } else {
                leaveRoom()
            }
            field = value
        }

    companion object {
        val firstDraftRegex = Regex("(?i)You used the Architect's First Draft to reset (?<name>.+?)!")
    }

    abstract val puzzleFilter: (CatacombPuzzleType?) -> Boolean
    fun matchesRoom(node: RoomNode?) = puzzleFilter(node?.backingData?.puzzleType)

    abstract fun enterRoom()
    abstract fun leaveRoom()
    abstract fun reset()

    @Subscription(inherited = true)
    context(event: CatacombRoomChangeEvent)
    fun onEnter() {
        isCurrentlyInPuzzle = matchesRoom(event.current)
    }

    @Subscription(inherited = true)
    context(event: CatacombRoomCheckmarkChangeEvent)
    fun onChat() {
        if (event.previous == CatacombRoomCheckmark.FAILED && matchesRoom(event.node)) {
            reset()
        }
    }

}
