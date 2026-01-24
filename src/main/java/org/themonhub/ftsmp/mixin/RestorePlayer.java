package org.themonhub.ftsmp.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.themonhub.ftsmp.Ftsmp;

@Mixin(net.minecraft.server.level.ServerPlayer.class)
public class RestorePlayer {
    @ModifyArg(method = "transferInventoryXpAndScore", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;replaceWith(Lnet/minecraft/world/entity/player/Inventory;)V", ordinal = 0))
    private Inventory restorePlayerInventory(Inventory inventory) {
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        Inventory voidDeathInv = Ftsmp.isVoidDeath.get(serverPlayer.getUUID());
        if (voidDeathInv != null) {
            return voidDeathInv;
        }
        return inventory;
    }

    @ModifyExpressionValue(
            method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            )
    )
    private <T> Object gameRuleOverwrite(T original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (Ftsmp.isVoidDeath.containsKey(player.getUUID())) {
            return true;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "die",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"
            )
    )
    private boolean isVoidDeath(boolean original) {
        return Ftsmp.isVoidDeath.containsKey(((ServerPlayer) (Object) this).getUUID()) || original;
    }

    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("TAIL"))
    private void cleanupVoidDeath(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Ftsmp.isVoidDeath.remove(player.getUUID());
    }
}
