package me.owdding.mortem.core.catacombs

import me.owdding.lib.utils.MeowddingLogger
import me.owdding.lib.utils.MeowddingLogger.Companion.featureLogger
import me.owdding.mortem.Mortem
import me.owdding.mortem.config.category.catacombs.CatacombsColorConfig
import me.owdding.mortem.config.category.catacombs.CatacombsMapConfig
import me.owdding.mortem.core.catacombs.types.CatacombClass
import me.owdding.mortem.utils.InterpolatedInt
import me.owdding.mortem.utils.extensions.toVector3d
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.level.saveddata.maps.MapDecoration
import org.joml.Vector2d
import org.joml.Vector2i
import org.joml.Vector3d
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonClass
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import tech.thatgravyboat.skyblockapi.utils.extentions.currentInstant
import tech.thatgravyboat.skyblockapi.utils.extentions.since
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

data class CatacombPlayer(var name: String, val catacomb: Catacomb): MeowddingLogger by Mortem.featureLogger() {

    var skin: PlayerSkin? = McPlayer.skin
    var isAlive: Boolean = true
    var uuid: UUID? = null
        set(value) {
            if (field != null) return
            field = value
        }
    val isSelf get() = uuid == McPlayer.uuid
    var dungeonClass: CatacombClass? = null
    var classLevel: Int? = null
    val player: AbstractClientPlayer? get() = if (isSelf) McClient.self.player else McLevel.self?.players()?.find { it.uuid == uuid }

    var realPosition: Vector3d? = null
    var realRotation: Float? = null
    var realUpdate: Instant = Instant.DISTANT_PAST
    val realDataUpToDate: Boolean get() = realUpdate.since() <= 100.milliseconds

    var mapPosition: Vector2i? = null
    var mapRotation: Byte? = null

    var minimapX by InterpolatedInt(CatacombsMapConfig::xEasingFunction, CatacombsMapConfig::xInterpolationTime)
    var minimapY by InterpolatedInt(CatacombsMapConfig::yEasingFunction, CatacombsMapConfig::yInterpolationTime)

    fun updateClass(catacombClass: String, classLevel: Int?) {
        this.isAlive = !(catacombClass == "DEAD" || catacombClass == "EMPTY")
        if (!isAlive) return
        this.dungeonClass = this.dungeonClass ?: CatacombClass.byName(catacombClass)
        this.classLevel = this.classLevel ?: classLevel
    }

    override fun toString(): String {
        return "CatacombPlayer(name='$name', isAlive=$isAlive, uuid=$uuid, isSelf=$isSelf, dungeonClass=$dungeonClass, classLevel=$classLevel, player=$player, realPosition=$realPosition, realRotation=$realRotation, realUpdate=$realUpdate, realDataUpToDate=$realDataUpToDate, mapPosition=$mapPosition, mapRotation=$mapRotation)"
    }

    fun updatePlayer(client: AbstractClientPlayer) {
        realPosition = client.position().toVector3d()
        realRotation = client.rotationVector.y
        realUpdate = currentInstant()
    }

    fun updateDecoration(decoration: MapDecoration) {
        this.mapPosition = Vector2i(decoration.x.toInt() + 128, decoration.y.toInt() + 128) // for some reason map decorations are centered around the center with (-128;-128) being the top left
        this.mapRotation = decoration.rot
    }
}
