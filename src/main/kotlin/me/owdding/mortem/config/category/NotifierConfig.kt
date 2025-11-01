package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.config.separator
import me.owdding.mortem.utils.colors.MortemColors
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.toFormattedName
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

object NotifierConfig : CategoryKt("notifier") {
    override val name = Translated("mortem.config.notifier")

    init {
        separator("mortem.config.notifier.max_secrets_notifier.separator")
    }

    var maxSecretsNotifier by boolean(true) {
        translation = "mortem.config.notifier.max_secrets_notifier"
    }

    var maxSecretsAnnounceType by enum(ChatType.CHAT) {
        translation = "mortem.config.notifier.max_secrets_announce_type"
    }



    // Score maybe..?
    // idek if this config category is a good idea

}

enum class ChatType {
    CHAT,
    PARTY;

    val formatted = toFormattedName()
    override fun toString() = formatted

    companion object {
        fun sendInType(type: ChatType, message: Component) {// TODO: sendWithPrefix
            when (type) {
                CHAT -> message.send()
                PARTY -> if (PartyAPI.inParty) {
                    Text.of("Sending message into party chat...", MortemColors.SEPARATOR).send()
                    McClient.sendCommand("/pc ${message.stripped}")
                } else message.send()
            }
        }
    }
}
