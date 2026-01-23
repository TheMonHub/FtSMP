package org.themonhub.ftsmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.HashSet;
import java.util.UUID;

public class Ftsmp implements ModInitializer {
    public static final String MOD_ID = "ftsmp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final HashSet<UUID> isVoidDeath = new HashSet<>();

    static int lastCheckedSeconds = secondsUntilNextUtcMidnight() - 1;
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
        Ftsmp.LOGGER.info("furryteens SMP is starting up!");
        ServerTickEvents.END_SERVER_TICK.register(Ftsmp::onTick);
        ServerPlayerEvents.LEAVE.register(Ftsmp::onLeave);
    }

    private static void onLeave(ServerPlayer player) {
        isVoidDeath.remove(player.getUUID());
    }

    private static void onTick(MinecraftServer minecraftServer) {
        int secondsLeft = secondsUntilNextUtcMidnight() - 1;

        if (Ftsmp.lastCheckedSeconds != secondsLeft) {
            lastCheckedSeconds = secondsLeft;

            // Behold! Ugly if-else nest!
            if (secondsLeft <= 0) {
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cShutting down..."), false);
                minecraftServer.stopServer();
            } else if (secondsLeft == 1) {
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in 1 second!"), false);
            } else if (secondsLeft <= 10) {
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in " + secondsLeft + " seconds!"), false);
            } else if (secondsLeft <= 60 && !didMinBroadcast) {
                didMinBroadcast = true;
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in 1 minute!"), false);
            } else if (secondsLeft <= 300 && !didFiveMinBroadcast) {
                didFiveMinBroadcast = true;
                minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§l§cServer shutting down in 5 minutes!"), false);
            } else if (secondsLeft <= 600 && !didTenMinBroadcast) {
            didTenMinBroadcast = true;
            minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal("§lServer shutting down in 10 minutes!"), false);
            }
        }
    }
}
