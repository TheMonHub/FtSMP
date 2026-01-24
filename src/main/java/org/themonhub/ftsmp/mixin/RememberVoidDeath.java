package org.themonhub.ftsmp.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.themonhub.ftsmp.Ftsmp;

@Mixin(net.minecraft.server.level.ServerPlayer.class)
public class RememberVoidDeath {
    @Inject(method = "die", at = @At("HEAD"))
    private void rememberVoidDeath(DamageSource source, CallbackInfo ci) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            ServerPlayer player = (ServerPlayer)(Object)this;
            ResourceKey<Level> dim = player.level().dimension();
            if (dim != Level.END) {
                Ftsmp.isVoidDeath.put(player.getUUID(), player.getInventory());
            }
        }
    }
}