package me.owdding.mortem.core.catacombs.secrets

import com.mojang.serialization.Codec
import me.owdding.ktcodecs.Compact
import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.ktcodecs.IncludedCodec
import me.owdding.mortem.generated.CodecUtils
import me.owdding.mortem.generated.MortemCodecs

@GenerateCodec
data class CatacombsSecretRoom(
    @Compact @FieldName("secret_groups") val secretGroups: List<CatacombsSecretGroup>,
) {
    fun reset() = secretGroups.forEach(CatacombsSecretGroup::reset)
}

data class CatacombsSecretGroup(
    val secrets: List<CatacombsSecret>,
) {
    val clicked: Boolean get() {
        val actualSecrets = secrets.filter { it.type.isSecret }
        if (actualSecrets.isEmpty()) return false
        return actualSecrets.all { it.clicked }
    }
    fun reset() = secrets.forEach(CatacombsSecret::reset)

    companion object {
        @IncludedCodec
        val CODEC: Codec<CatacombsSecretGroup> =
            CodecUtils.compactList(MortemCodecs.getCodec<CatacombsSecret>()).xmap(::CatacombsSecretGroup, CatacombsSecretGroup::secrets)
    }
}
