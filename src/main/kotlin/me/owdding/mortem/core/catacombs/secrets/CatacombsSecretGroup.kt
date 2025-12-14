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
    val id: String,
    @Compact @FieldName("secret_groups") val secretGroups: MutableList<CatacombsSecretGroup> = mutableListOf(),
) {
    fun markDirty() {
        needsUpdate = true
    }
    var needsUpdate = true
    fun addSecret(secret: CatacombsSecret) {
        removeSecret(secret)
        secretGroups.add(CatacombsSecretGroup(secret))
        markDirty()
    }
    fun removeSecret(secret: CatacombsSecret) {
        secretGroups.forEach { it.secrets.remove(secret) }
        removeEmpty()
        markDirty()
    }
    fun replaceSecret(old: CatacombsSecret, new: CatacombsSecret) {
        secretGroups.forEach { it.replaceSecret(old, new) }
        markDirty()
    }

    private fun removeEmpty() = secretGroups.removeIf { it.secrets.isEmpty() }
    fun reset() = secretGroups.forEach(CatacombsSecretGroup::reset)
}

data class CatacombsSecretGroup(
    val secrets: MutableList<CatacombsSecret> = mutableListOf(),
) {
    constructor(secret: CatacombsSecret) : this(mutableListOf(secret))
    fun replaceSecret(old: CatacombsSecret, new: CatacombsSecret) {
        val index = secrets.indexOf(old)
        if (index != -1) secrets[index] = new
    }

    val clicked: Boolean get() {
        val actualSecrets = secrets.filter { it.type.isSecret }
        if (actualSecrets.isEmpty()) return false
        return actualSecrets.all { it.clicked }
    }
    fun reset() = secrets.forEach(CatacombsSecret::reset)

    companion object {
        @IncludedCodec
        val CODEC: Codec<CatacombsSecretGroup> =
            CodecUtils.compactMutableList(MortemCodecs.getCodec<CatacombsSecret>()).xmap(::CatacombsSecretGroup, CatacombsSecretGroup::secrets)
    }
}
