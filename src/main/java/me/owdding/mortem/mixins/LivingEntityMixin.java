package me.owdding.mortem.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.owdding.mortem.events.EntityDeathEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @WrapMethod(method = "die")
    private void onDieWrap(DamageSource damageSource, Operation<Void> original) {
        new EntityDeathEvent((LivingEntity) (Object) this, damageSource).post(SkyBlockAPI.getEventBus());
        original.call(damageSource);
    }

}
