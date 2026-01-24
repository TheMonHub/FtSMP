package org.themonhub.ftsmp;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.HashMap;
import java.util.UUID;

public class Ftsmp implements ModInitializer {
    public static final String MOD_ID = "ftsmp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final HashMap<UUID, Inventory> isVoidDeath = new HashMap<>();

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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("ftsmp")
                    .then(Commands.literal("help")
                            .executes(ctx -> {
                                var source = ctx.getSource();
                                var node = dispatcher.getRoot().getChild("ftsmp");
                                dispatcher.getSmartUsage(node, source).forEach((k, v) ->
                                        source.sendSuccess(() -> Component.literal("/ftsmp " + v), false)
                                );
                                return 1;
                            })
                    )
                    .then(Commands.literal("version").executes(Ftsmp::executeVersionCommand))
            );
        });
    }

    public static String getVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("UNKNOWN");
    }

    private static int executeVersionCommand(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        commandSourceStackCommandContext.getSource().sendSuccess(() -> Component.literal("FtSMP version: " + getVersion()), false);
        return 1;
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
                minecraftServer.halt(false);
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
