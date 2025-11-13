package me.owdding.mortem.utils

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandExceptionType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import me.owdding.mortem.core.catacombs.Catacomb
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.color

// Even though its unused, we make the functions have CommandContext as a receiver parameter to make it so you can only
// throw them inside commands.
@Suppress("UnusedReceiverParameter")
object CommandExceptions : CommandExceptionType {
    fun create(message: String, error: Boolean = true): CommandSyntaxException {
        val text = Text.of(message) {
            if (error) color = TextColor.RED
        }
        return CommandSyntaxException(this, text)
    }
    fun create(component: Component) = CommandSyntaxException(this, component)

    fun CommandContext<*>.getCatacomb(): Catacomb = CatacombsManager.catacomb ?: throw create("Not in a catacomb!")

    fun CommandContext<*>.assertInCatacombs() {
        if (CatacombsManager.catacomb == null) throw create("Not in a catacomb!")
    }

    fun CommandContext<*>.getCatacombsRoom(): RoomNode = CatacombsManager.catacomb?.lastRoom ?: throw create("Not in a room!")

    fun CommandContext<*>.assertInCatacombsRoom() {
        if (CatacombsManager.catacomb?.lastRoom == null) throw create("Not in a room!")
    }


}
