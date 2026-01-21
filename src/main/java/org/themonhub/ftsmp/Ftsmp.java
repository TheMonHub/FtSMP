package org.themonhub.ftsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.time.*;

public class Ftsmp implements ModInitializer {
    static int lastCheckedSeconds = secondsUntilNextUtcMidnight();
    static boolean didMinBroadcast = false;
    static boolean didFiveMinBroadcast = false;
    static boolean didTenMinBroadcast = false;
    public static int secondsUntilNextUtcMidnight() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        ZonedDateTime nextMidnight = now
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC);

        return Math.toIntExact(Duration.between(now, nextMidnight).getSeconds());
    }

    @Override
    public void onInitialize() {
        System.out.println("furryteens SMP!");
        ServerTickEvents.END_SERVER_TICK.register(Ftsmp::onTick);
    }

    private static void onTick(MinecraftServer minecraftServer) {
        int secondsLeft = secondsUntilNextUtcMidnight();

        if (Ftsmp.lastCheckedSeconds != secondsLeft) {
            lastCheckedSeconds = secondsLeft;

            // Behold! Ugly if-else nest!
            if (secondsLeft <= 0) {
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cShutting down..."), false);
                minecraftServer.close();
            } else if (secondsLeft == 1) {
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in 1 second!"), false);
            } else if (secondsLeft <= 10) {
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in " + secondsLeft + " seconds!"), false);
            } else if (secondsLeft <= 60 && !didMinBroadcast) {
                didMinBroadcast = true;
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in 1 minute!"), false);
            } else if (secondsLeft <= 300 && !didFiveMinBroadcast) {
                didTenMinBroadcast = true;
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in 5 minutes!"), false);
            } else if (secondsLeft <= 600 && !didTenMinBroadcast) {
            didTenMinBroadcast = true;
            minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§lServer shutting down in 10 minutes!"), false);
            }
        }
    }
}
