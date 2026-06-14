package me.owdding.mortem.utils.extensions

import net.minecraft.client.multiplayer.PlayerInfo
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor

val PlayerInfo.column: Int get() = this.profile.name[1] - 'A'
val PlayerInfo.column_: Int get() = (this.profile.name[1] - 65).code
val PlayerInfo.row: Int get() = this.profile.name[3] - 'a'

val DungeonFloor.masterMode: Boolean get() = ordinal > 7
val DungeonFloor.endText: String get() = "${if (masterMode) "Master Mode " else ""}The Catacombs"
