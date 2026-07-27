package me.owdding.mortem.config.category

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.config.AutoTranslated
import me.owdding.mortem.config.separator
import me.owdding.mortem.utils.colors.MortemColors
import me.owdding.mortem.utils.extensions.sendWithPrefix
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.profile.party.PartyAPI
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.extentions.toFormattedName
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.Text.send
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

object NotifierConfig : CategoryKt("notifier"), AutoTranslated {
    override val translationBase: String = "mortem.config.notifier"
    override val name = Translated(translationBase)

    init {
        separator("${translationBase}.max_secrets_notifier.separator")
    }

    var maxSecretsNotifier by autoBoolean("max_secrets_notifier", true)

    var maxSecretsAnnounceType by autoEnum("max_secrets_notifier-announce_type", ChatType.CHAT)

    // Score maybe..?
    // idek if this config category is a good idea

}

enum class ChatType {
    CHAT,
    PARTY;

    val formatted = toFormattedName()
    override fun toString() = formatted

    companion object {
        fun sendInType(type: ChatType, message: Component) {
            when (type) {
                CHAT -> message.sendWithPrefix()
                PARTY -> if (PartyAPI.inParty) {
                    Text.of("Sending message into party chat...", MortemColors.SEPARATOR).send()
                    McClient.sendCommand("/pc ${message.stripped}")
                } else message.sendWithPrefix()
            }
        }
    }
}
