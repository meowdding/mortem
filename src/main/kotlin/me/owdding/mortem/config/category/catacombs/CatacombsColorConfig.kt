package me.owdding.mortem.config.category.catacombs

import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement
import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import com.teamresourceful.resourcefulconfigkt.api.RConfigKtEntry
import me.owdding.mortem.config.AutoTranslated
import me.owdding.mortem.config.ResettableCategory
import me.owdding.mortem.config.remember
import me.owdding.mortem.utils.colors.CatppuccinColors

object CatacombsColorConfig : CategoryKt("catacombs"), AutoTranslated, ResettableCategory {
    override val translationBase: String = "mortem.config.catacombs.map"

    override val entries: MutableList<RConfigKtEntry<*>> = mutableListOf()

    var archer by color(0xff0000).remember()
    var berserker by color(0xffaa00).remember()
    var healer by color(0xff00aa).remember()
    var mage by color(0x0000ff).remember()
    var tank by color(0x00ff00).remember()

    var normalRoom by autoColor(0xAb6b00).remember()
    var rareRoom by autoColor(0x0000FF).remember()
    var trapRoom by autoColor(0xFF7F0F).remember()
    var fairyRoom by autoColor(0xF080FF).remember()
    var puzzleRoom by autoColor(0xe050F0).remember()
    var minibossRoom by autoColor(0xFFFF00).remember()
    var bloodRoom by autoColor(0xFF0000).remember()
    var startRoom by autoColor(0x00FF00).remember()
    var unknownRoom by autoColor(0xababab).remember()
    var defaultRoom by autoColor(0x000000).remember()

    var clearedCheckmark by autoColor(0xffffff).remember()
    var completeCheckmark by autoColor(0x00ff00).remember()
    var failedCheckmark by autoColor(0xff0000).remember()
    var noneCheckmark by autoColor(0xababab).remember()

    var witherDoor by autoColor(0x4f4f4f).remember()
    var bloodDoor by autoColor(0xFF0000).remember()
    var normalDoor by autoColor(0xab6b00).remember()
    var trapDoor by autoColor(0xff7f0f).remember()
    var minibossDoor by autoColor(0xFFFF00).remember()
    var puzzleDoor by autoColor(0xe060f0).remember()
    var fairyDoor by autoColor(0xf080ff).remember()
    var defaultDoor by autoColor(0x000000).remember()

    init {
        autoButton("reset") { onClick { entries.forEach { it.reset() } } }
        autoButton("catppuccin") {
            onClick {
                archer = CatppuccinColors.Mocha.red
                berserker = CatppuccinColors.Mocha.peach
                healer = CatppuccinColors.Mocha.mauve
                mage = CatppuccinColors.Mocha.sky
                tank = CatppuccinColors.Mocha.green

                normalRoom = CatppuccinColors.Mocha.flamingo
                rareRoom = CatppuccinColors.Mocha.sky
                trapRoom = CatppuccinColors.Mocha.peach
                fairyRoom = CatppuccinColors.Mocha.pink
                puzzleRoom = CatppuccinColors.Mocha.mauve
                minibossRoom = CatppuccinColors.Mocha.yellow
                bloodRoom = CatppuccinColors.Mocha.red
                startRoom = CatppuccinColors.Mocha.green
                unknownRoom = CatppuccinColors.Mocha.surface2
                defaultRoom = CatppuccinColors.Mocha.surface0

                clearedCheckmark = CatppuccinColors.Mocha.text
                completeCheckmark = CatppuccinColors.Mocha.green
                failedCheckmark = CatppuccinColors.Mocha.red
                noneCheckmark = CatppuccinColors.Mocha.subtext0

                witherDoor = CatppuccinColors.Mocha.base
                bloodDoor = bloodRoom
                normalDoor = normalRoom
                trapDoor = trapRoom
                minibossDoor = minibossRoom
                puzzleDoor = puzzleRoom
                fairyDoor = fairyRoom
                defaultDoor = defaultRoom
            }
        }
    }

}
