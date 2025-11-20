package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.mortem.events.EntityDeathEvent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.monster.Zombie
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonAPI
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyWidget
import tech.thatgravyboat.skyblockapi.api.events.chat.ChatReceivedEvent
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidget
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidgetChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.enumMapOf
import tech.thatgravyboat.skyblockapi.utils.extentions.toFloatValue
import tech.thatgravyboat.skyblockapi.utils.extentions.toIntValue
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/*
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

// TODO
//  Spirit Pet in Death
//  Prince Kill
//  Entrace req
@Module
object ScoreCalculator {
    // <editor-fold desc="Regex Wall">
    // --Chat--
    private val mimicKilledRegex = ".*(\$SKYTILS-DUNGEON-SCORE-MIMIC\$|Mimic [Dd]ead!|Mimic Killed!)$".toRegex()
    // --Scoreboard--
    private val clearedPercentageRegex = "\\s*Cleared: (?<percentage>[\\d,.]+)% \\((?<score>[\\d.]+)\\)".toRegex()
    // -- Tab--
    private val cryptsRegex = " Crypts: (?<amount>\\d+)".toRegex()
    private val secretPercentageRegex = " Secrets Found: (?<percentage>[\\d.]+)%".toRegex()
    private val puzzleRegex = " [\\w\\s?]+: \\[[✖✦]](?: \\(.*\\))?".toRegex()
    // </editor-fold>

    private val requirements = enumMapOf(
        DungeonFloor.E to Requirements(0.3, 600),
        DungeonFloor.F1 to Requirements(0.3, 600),
        DungeonFloor.F2 to Requirements(0.4, 600),
        DungeonFloor.F3 to Requirements(0.5, 600),
        DungeonFloor.F4 to Requirements(0.6, 720),
        DungeonFloor.F5 to Requirements(0.7, 600),
        DungeonFloor.F6 to Requirements(0.85, 720),
        DungeonFloor.F7 to Requirements(1.0, 840),
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

    private var deaths = 0
    private var secretPercentage = 0f
    private var roomsClearedPercentage = 0f
    private var mimicKilled = false
    private var cryptsKilled = 0
    private var failedPuzzles = 0

    fun getScore() = DungeonAPI.dungeonFloor?.let { getScore(DungeonAPI.time, it) }
    fun getScore(time: Duration, floor: DungeonFloor): Score {
        val req = requirements[floor] ?: return Score.ZERO
        return Score(
            getSkillScore(),
            getExplorationScore(req),
            getSpeedScore(time, req),
            getBonusScore(),
        )
    }

    private fun getSkillScore(): Int {
        val roomsScore = floor((80 * roomsClearedPercentage)).roundToInt()
        val puzzlePenalty = 10 * failedPuzzles
        val deathPenalty = 2 * deaths

        return 20 + roomsScore - puzzlePenalty - deathPenalty
    }

    private fun getExplorationScore(req: Requirements): Int {
        val roomsScore = floor(60 * roomsClearedPercentage).roundToInt()
        val secretsScore = floor((40 * (secretPercentage / req.secretPercentNeeded).coerceAtMost(1.0))).roundToInt()
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
        if (Perk.EZPZ.active) add(10)
        if (mimicKilled) add(2)
        add(cryptsKilled.coerceAtMost(5))
    }.sum()

    data class Score(
        val skill: Int,
        val exploration: Int,
        val speed: Int,
        val bonus: Int,
    ) {
        val total = skill + exploration + speed + bonus
        val rank = Rank.getRank(total)

        companion object {
            val ZERO = Score(0,0,0,0)
        }
    }

    private data class Requirements(
        val secretPercentNeeded: Double,
        val speedTime: Duration,
    ) {
        constructor(secretPercentNeeded: Double, speedTime: Int) : this(secretPercentNeeded, speedTime.seconds)
    }

    enum class Rank(val minScore: Int, val component: Component) {
        D(0, Text.of("D").withColor(TextColor.RED)),
        C(100, Text.of("C").withColor(TextColor.BLUE)),
        B(160, Text.of("B").withColor(TextColor.GREEN)),
        A(230, Text.of("A").withColor(TextColor.DARK_PURPLE)),
        S(270, Text.of("S").withColor(TextColor.YELLOW)),
        S_PLUS(300, Text.of("S+").withColor(TextColor.GOLD).withStyle(ChatFormatting.BOLD));

        companion object {
            fun getRank(score: Int) = entries.lastOrNull { score >= it.minScore } ?: D
        }
    }

    private fun isMimicFloor() = DungeonAPI.dungeonFloor in listOf(DungeonFloor.F6, DungeonFloor.F7, DungeonFloor.M6, DungeonFloor.M7)

    @Subscription(ServerChangeEvent::class, ServerDisconnectEvent::class)
    fun reset() {
        deaths = 0
        secretPercentage = 0f
        roomsClearedPercentage = 0f
        mimicKilled = false
        cryptsKilled = 0
        failedPuzzles = 0
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun onEntityDeath(event: EntityDeathEvent) {
        if (event.entity !is Zombie || !event.entity.isBaby || !isMimicFloor()) return
        McClient.runNextTick {
            if (mimicKilled) return@runNextTick
            McClient.sendCommand("pc Mimic Killed!")
            mimicKilled = true
        }
    }

    @Subscription
    @OnlyIn(SkyBlockIsland.THE_CATACOMBS)
    fun onChat(event: ChatReceivedEvent.Pre) {
        matchWhen(event.text) {
            case(mimicKilledRegex) {
                if (isMimicFloor()) mimicKilled = true
            }
        }
    }

    @Subscription
    fun onScoreboard(event: ScoreboardUpdateEvent) {
        clearedPercentageRegex.anyMatch(event.new, "percentage") { (percentage) ->
            roomsClearedPercentage = percentage.toFloatValue() / 100f
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
            this.secretPercentage = percentage.toFloatValue() / 100f
        }
    }

    @Subscription
    @OnlyWidget(TabWidget.PUZZLES)
    fun onPuzzlesWidget(event: TabWidgetChangeEvent) {
        failedPuzzles = event.new.count { puzzleRegex.matches(it) }
    }

    @Subscription
    @OnlyWidget(TabWidget.TEAM_DEATHS)
    fun onTeamDeathsWidget(event: TabWidgetChangeEvent) {
        TabWidget.TEAM_DEATHS.regex.anyMatch(event.new, "amount") { (amount) ->
            this.deaths = amount.toIntValue()
        }
    }
}
