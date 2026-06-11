package me.owdding.mortem.core.catacombs

import me.owdding.mortem.core.catacombs.types.CatacombClass
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import org.joml.Vector2d
import org.joml.Vector3d
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonClass
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.helpers.McPlayer
import java.util.UUID

data class CatacombPlayer(var name: String) {
    var isAlive: Boolean = true
    var uuid: UUID? = null
    val isSelf = uuid == McPlayer.uuid
    val dungeonClass: CatacombClass? = null
    val player: AbstractClientPlayer? get() = if (isSelf) McClient.self.player else McLevel.self?.players()?.find { it.uuid == uuid }

    val realPosition: Vector3d? = null
    val realRotation: Byte? = null
    val realDataUpToDate: Boolean = false

    val mapPosition: Vector2d? = null
    val mapRotation: Byte? = null
}
