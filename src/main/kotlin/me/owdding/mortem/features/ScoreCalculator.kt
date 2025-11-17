package me.owdding.mortem.features

import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyWidget
import tech.thatgravyboat.skyblockapi.api.events.chat.ChatReceivedEvent
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidget
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidgetChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.extentions.toFloatValue
import tech.thatgravyboat.skyblockapi.utils.extentions.toIntValue
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*

// --- SKILL --- max 100
The Skill score ranges from 20 to 100, with 20 being the lowest score achievable. The Skill Score is calculated as such:
Skill=20+⌊80*(CompletedRoomsTotalRooms)⌋−(10*FailedPuzzles)−DeathPenalty
Each death 2, except first death with spirit pet

// --- EXPLORATION --- max 100
The Exploration Score depends on the percentage of dungeon completed, along with the percentage of secrets achieved. It is calculated as such:
Explore=⌊60*(CompletedRooms/TotalRooms)⌋+⌊(40*SecretPercentFound/SecretPercentNeeded)⌋


Discoveries: 4
 Secrets Found: 4
 Crypts: 0

Dungeon: Catacombs
 Opened Rooms: 34
 Completed Rooms: 16
 Secrets Found: 7.8%
 Time: 06m 39s

Puzzles: (3)
 Quiz: [✖] ()
 Three Weirdos: [✔]
 ???: [✦]

 */

object ScoreCalculator {
    // <editor-fold desc="Regex Wall">
    // --Chat--
    private val mimicKilledRegex = ".*(\$SKYTILS-DUNGEON-SCORE-MIMIC\$|Mimic [Dd]ead!|Mimic Killed!)$".toRegex()
    // -- Tab--
    private val cryptsRegex = " Crypts: (?<amount>\\d+)".toRegex()
    private val secretPercentageRegex = " Secrets Found: (?<percentage>[\\d.]+)%".toRegex()
    // </editor-fold>

    private val requirements = mapOf(
        DungeonFloor.E to Requirements(0.3,600), // TODO
        DungeonFloor.F1 to Requirements(0.3,600),
        DungeonFloor.F2 to Requirements(0.4,600),
        DungeonFloor.F3 to Requirements(0.5,600),
        DungeonFloor.F4 to Requirements(0.6,720),
        DungeonFloor.F5 to Requirements(0.7,600),
        DungeonFloor.F6 to Requirements(0.85,720),
        DungeonFloor.F7 to Requirements(1.0,840),
        DungeonFloor.M1 to Requirements(1.0, 480),
        DungeonFloor.M2 to Requirements(1.0, 480),
        DungeonFloor.M3 to Requirements(1.0, 480),
        DungeonFloor.M4 to Requirements(1.0, 480),
        DungeonFloor.M5 to Requirements(1.0, 480),
        DungeonFloor.M6 to Requirements(1.0, 600),
        DungeonFloor.M7 to Requirements(1.0, 840),
    )
    private val speedDecreasePercentage = mapOf(
        0.0..0.2 to 0.02,
        0.2..0.4 to 0.04,
        0.4..0.5 to 0.05,
        0.5..0.6 to 0.06,
        0.6..Double.MAX_VALUE to 0.07,
    )

    private var mimicKilled = false
    private var cryptsKilled = 0
    private var secretPercentage = 0f

    fun getScore(time: Duration, floor: DungeonFloor): Int {
        val req = requirements[floor] ?: return 0
        return listOf(
            getSkillScore(),
            getExplorationScore(req),
            getSpeedScore(time, req),
            getBonusScore(),
        ).sum()
    }

    private fun getSkillScore(): Int {
        return 0
    }

    private fun getExplorationScore(req: Requirements): Int {
        val roomsScore = 0
        val secretsScore = floor((40 * secretPercentage / req.secretPercentNeeded)).roundToInt()
        return roomsScore + secretsScore
    }

    private fun getSpeedScore(time: Duration, req: Requirements): Int {
        if (time <= req.speedTime) return 100

        val percentOver = (time / req.speedTime) - 1
        val lost = speedDecreasePercentage.mapNotNull { (range, step) ->
            if (percentOver < range.start) return@mapNotNull null
            val delta = (percentOver - range.start).coerceAtMost(range.endInclusive - range.start)
            delta / step
        }
        return 100 - floor(lost.sum()).roundToInt()
    }

    private fun getBonusScore(): Int = buildList {
        // TODO: prince maybe
        if (Perk.EZPZ.active) add(10)
        if (mimicKilled) add(2)
        add(cryptsKilled.coerceAtMost(5))
    }.sum()

    data class Requirements(
        val secretPercentNeeded: Double,
        val speedTime: Duration,
    ) {
        constructor(secretPercentNeeded: Double, speedTime: Int) : this(secretPercentNeeded, speedTime.seconds)
    }

    @Subscription(ServerChangeEvent::class, ServerDisconnectEvent::class)
    fun reset() {
        mimicKilled = false
        cryptsKilled = 0
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun onChat(event: ChatReceivedEvent.Pre) {
        matchWhen(event.text) {
            case(mimicKilledRegex) {
                mimicKilled = true
            }
        }
    }

    @Subscription
    @OnlyWidget(TabWidget.DISCOVERIES)
    fun onDiscoveriesWidget(event: TabWidgetChangeEvent) {
        cryptsRegex.anyMatch(event.new, "amount") { (amount) ->
            cryptsKilled = amount.toIntValue()
        }
    }

    @Subscription
    @OnlyWidget(TabWidget.AREA)
    fun onAreaWidget(event: TabWidgetChangeEvent) {
        secretPercentageRegex.anyMatch(event.new, "percentage") { (percentage) ->
            this.secretPercentage == percentage.toFloatValue() / 100f
        }
    }

    @Subscription
    @OnlyWidget(TabWidget.PUZZLES)
    fun onPuzzlesWidget(event: TabWidgetChangeEvent) {

    }
}
