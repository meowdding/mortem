package me.owdding.mortem.utils.extensions

import eu.pb4.placeholders.api.node.parent.GradientNode
import me.owdding.mortem.utils.colors.CatppuccinColors
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor as McTextColor
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

val PREFIX = Text.of {
    append("«")
    append(Text.of("Mortem").gradient(
        CatppuccinColors.Mocha.red, CatppuccinColors.Mocha.yellow
    ))
    append("»")
    this.color = TextColor.GRAY
}

fun Component.sendWithPrefix() = Text.join(PREFIX, " ", this).send()
fun Component.sendWithPrefix(id: String) = Text.join(PREFIX, " ", this).send(id)

fun MutableComponent.gradient(vararg colors: Int): Component = GradientNode.apply(
    this,
    GradientNode.GradientProvider.colors(colors.map { McTextColor.fromRgb(it) })
)
