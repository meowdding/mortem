package me.owdding.mortem.utils.extensions

import net.minecraft.client.multiplayer.PlayerInfo

val PlayerInfo.column: Int get() = this.profile.name[1] - 'A'
val PlayerInfo.column_: Int get() = (this.profile.name[1] - 65).code
val PlayerInfo.row: Int get() = this.profile.name[3] - 'a'
