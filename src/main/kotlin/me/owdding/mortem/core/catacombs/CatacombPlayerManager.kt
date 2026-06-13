package me.owdding.mortem.core.catacombs

import me.owdding.ktmodules.Module
import me.owdding.lib.utils.MeowddingLogger
import me.owdding.lib.utils.MeowddingLogger.Companion.featureLogger
import me.owdding.mortem.Mortem
import me.owdding.mortem.core.catacombs.types.CatacombClass
import me.owdding.mortem.utils.extensions.column
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.TimePassed
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanOrArabic
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.match
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object CatacombPlayerManager : MeowddingLogger by Mortem.featureLogger() {

    val playerPattern = "^\\[(?<level>\\d+)] (?:\\[\\w+] )?(?<name>\\w+) (?:.{0,2} )?\\((?<class>\\w+)(?: (?<classLevel>\\w+))?\\)$".toRegex()

    @TimePassed("2t")
    @Subscription
    context(_: TickEvent)
    fun tick() {
        val catacomb: Catacomb = CatacombsManager.catacomb ?: return
        val self = McPlayer.self ?: return
        val level = McLevel.self ?: return

        var ownPlayer: CatacombPlayer? = null

        // reset current player lists
        catacomb.playerList.fill(null)
        var index = 0
        McClient.tablist.forEach {
            if (it.column != 0) return@forEach
            val name = it.tabListDisplayName?.stripped ?: return@forEach
            val match = playerPattern.matchEntire(name) ?: return@forEach
            val playerName = match.groups["name"]?.value ?: return@forEach
            val sbLevel = match.groups["level"]?.value?.toInt()
            val catacombClass = match.groups["class"]?.value ?: return@forEach
            val classLevel = match.groups["classLevel"]?.value?.parseRomanOrArabic()

            catacomb.getOrRegisterPlayer(playerName) {
                CatacombPlayer(playerName, catacomb)
            }.apply {
                this.skin = it.skin
                updateClass(catacombClass, classLevel)
                if (McPlayer.name == this.name) {
                    ownPlayer = this
                    this.uuid = self.uuid
                    return@apply
                }
                catacomb.playerList[index++] = this
            }
        }

        level.players().forEach {
            if (it.uuid.version() != 4) return@forEach
            catacomb.getOrRegisterPlayer(it.gameProfile.name) {
                CatacombPlayer(it.gameProfile.name, catacomb)
            }.apply {
                this.uuid = it.uuid
                this.skin = it.skin
                this.updatePlayer(it)
            }
        }

        if (ownPlayer != null) {
            catacomb.playerList[index] = ownPlayer
        }
    }
}
