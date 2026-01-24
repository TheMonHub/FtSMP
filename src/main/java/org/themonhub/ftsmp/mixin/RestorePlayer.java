package org.themonhub.ftsmp.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.themonhub.ftsmp.Ftsmp;

@Mixin(net.minecraft.server.level.ServerPlayer.class)
public class RestorePlayer {
    @ModifyExpressionValue(
            method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            )
    )
    private <T> Object gameRuleOverwrite(T original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (Ftsmp.isVoidDeath.contains(player.getUUID())) {
            return true;
        }
        return original;
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void cleanupVoidDeath(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Ftsmp.isVoidDeath.remove(player.getUUID());
    }
}
