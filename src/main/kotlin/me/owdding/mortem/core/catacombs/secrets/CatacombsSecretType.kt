package me.owdding.mortem.core.catacombs.secrets

import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.mortem.generated.DispatchHelper
import net.minecraft.world.phys.AABB
import org.joml.Vector3ic
import kotlin.reflect.KClass

enum class CatacombsSecretType(
    override val type: KClass<out CatacombsSecret>,
    val constructor: (Vector3ic) -> CatacombsSecret,
    val isSecret: Boolean = true,
) : DispatchHelper<CatacombsSecret> {
    CHEST(ChestSecret::class, ::ChestSecret),
    ITEM(ItemSecret::class, ::ItemSecret),
    BAT(BatSecret::class, ::BatSecret),
    ESSENCE(EssenceSecret::class, ::EssenceSecret),

    LEVER(LeverSecret::class, { LeverSecret(it, fullBlockAABB) }, isSecret = false),
    REDSTONE_KEY(RedstoneKeySecret::class, ::RedstoneKeySecret, isSecret = false),

    NONE(NoSecret::class, ::NoSecret, isSecret = false),
    ;

    val ignore: Boolean get() = this == NONE
}

sealed class CatacombsSecret(val type: CatacombsSecretType) {
    abstract var pos: Vector3ic
    abstract val aabb: AABB

    var clicked: Boolean = false
        private set

    fun click() {
        clicked = true
    }
    fun reset() {
        clicked = false
    }
}

sealed class PlayerHeadSecret(type: CatacombsSecretType) : CatacombsSecret(type) {
    override val aabb: AABB get() = skullAABB
}

@GenerateCodec
data class ChestSecret(
    override var pos: Vector3ic,
) : CatacombsSecret(CHEST) {
    override val aabb: AABB get() = chestAABB
}

@GenerateCodec
data class ItemSecret(
    override var pos: Vector3ic,
) : CatacombsSecret(ITEM) {
    override val aabb: AABB get() = skullAABB
}

@GenerateCodec
data class BatSecret(
    override var pos: Vector3ic,
) : CatacombsSecret(BAT) {
    override val aabb: AABB get() = fullBlockAABB
}

@GenerateCodec
data class EssenceSecret(
    override var pos: Vector3ic,
) : PlayerHeadSecret(ESSENCE)

@GenerateCodec
data class RedstoneKeySecret(
    override var pos: Vector3ic,
    /** [pickUp] is `true` when the waypoint indicates the position the redstone key needs to be picked up at */
    @FieldName("pick_up") var pickUp: Boolean = true,
) : PlayerHeadSecret(REDSTONE_KEY)

@GenerateCodec
data class LeverSecret(
    override var pos: Vector3ic,
    override var aabb: AABB,
) : CatacombsSecret(LEVER)

@GenerateCodec
data class NoSecret(
    override var pos: Vector3ic,
) : CatacombsSecret(NONE) {
    override val aabb: AABB get() = fullBlockAABB
}

private val fullBlockAABB = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
private val chestAABB = AABB(0.0625,0.0,0.0625,0.9375,0.875,0.9375)
private val skullAABB = AABB(0.25, 0.0, 0.25, 0.75, 0.5, 0.75)
