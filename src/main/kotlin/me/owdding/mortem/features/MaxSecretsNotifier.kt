package me.owdding.mortem.features

import me.owdding.ktmodules.Module
import me.owdding.mortem.config.category.ChatType
import me.owdding.mortem.config.category.NotifierConfig
import me.owdding.mortem.utils.colors.MortemColors
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyWidget
import tech.thatgravyboat.skyblockapi.api.events.dungeon.DungeonEnterEvent
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidget
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidgetChangeEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.regex.component.ComponentRegex
import tech.thatgravyboat.skyblockapi.utils.regex.component.anyMatch
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

@Module
object MaxSecretsNotifier {

    private var announced = false
    private val regex = ComponentRegex(" Secrets Found: (?<found>[\\d+,.]+%)")

    @Subscription
    fun onDungeonJoin(event: DungeonEnterEvent) {
        announced = false
    }

    @Subscription
    @OnlyWidget(TabWidget.AREA)
    fun onTabWidget(event: TabWidgetChangeEvent) {
        if (!NotifierConfig.maxSecretsNotifier || announced) return
        regex.anyMatch(event.newComponents, "found") { (found) ->
            if (found.color == TextColor.GREEN) {
                announced = true
                val text = Text.of("Max Secrets Reached!", MortemColors.HIGHLIGHT)
                ChatType.sendInType(NotifierConfig.maxSecretsAnnounceType, text)
                McClient.setTitle(text, stayTime = 2f)
            }
        }
    }

}
