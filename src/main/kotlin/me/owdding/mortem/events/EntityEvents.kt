package me.owdding.mortem.events

import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent

data class EntityDeathEvent(
    val entity: LivingEntity,
    val damageSource: DamageSource,
) : SkyBlockEvent()
