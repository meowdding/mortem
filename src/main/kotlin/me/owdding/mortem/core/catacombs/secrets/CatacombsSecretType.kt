package me.owdding.mortem.core.catacombs.secrets

import me.owdding.ktcodecs.FieldName
import me.owdding.ktcodecs.GenerateCodec
import me.owdding.ktcodecs.GenerateDispatchCodec
import me.owdding.mortem.core.catacombs.CatacombsColorProvider
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.generated.DispatchHelper
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.toVec3
import net.minecraft.world.phys.AABB
import org.joml.Vector3ic
import tech.thatgravyboat.skyblockapi.utils.extentions.valueOfOrNull
import kotlin.reflect.KClass

// TODO: make colors not be hardcoded
@GenerateDispatchCodec(CatacombsSecret::class)
enum class CatacombsSecretType(
    override val type: KClass<out CatacombsSecret>,
    val constructor: (Vector3ic) -> CatacombsSecret,
    colorProvider: CatacombsColorProvider,
    val isSecret: Boolean = true,
) : DispatchHelper<CatacombsSecret>, CatacombsColorProvider by colorProvider {
    CHEST(ChestSecret::class, ::ChestSecret, CatppuccinColors.Latte::base),
    ITEM(ItemSecret::class, ::ItemSecret, CatppuccinColors.Mocha::sapphire),
    BAT(BatSecret::class, ::BatSecret, CatppuccinColors.Frappe::peach),
    ESSENCE(EssenceSecret::class, ::EssenceSecret, CatppuccinColors.Latte::mauve),

    LEVER(LeverSecret::class, { LeverSecret(it, fullBlockAABB) }, { 0xf2da6d }, isSecret = false),
    REDSTONE_KEY(RedstoneKeySecret::class, ::RedstoneKeySecret, CatppuccinColors.Macchiato::red, isSecret = false),

    NONE(NoSecret::class, ::NoSecret, CatppuccinColors.Mocha::rosewater, isSecret = false),
    ;

    val ignore: Boolean get() = this == NONE

    companion object {
        fun getType(string: String): CatacombsSecretType = valueOfOrNull<CatacombsSecretType>(string.uppercase()) ?: NONE
    }
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

    fun realPos(room: RoomNode): Vector3ic = room.roomToWorld(pos)
    fun realAABB(room: RoomNode): AABB = aabb.move(realPos(room).toVec3())
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
private val chestAABB = AABB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375)
private val skullAABB = AABB(0.25, 0.0, 0.25, 0.75, 0.5, 0.75)
