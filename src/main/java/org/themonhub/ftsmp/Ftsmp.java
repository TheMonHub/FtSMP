package org.themonhub.ftsmp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Inventory;
import org.geysermc.geyser.api.GeyserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.*;
import java.util.*;

public class Ftsmp implements ModInitializer {
    public static final String MOD_ID = "ftsmp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final HashMap<UUID, Inventory> isVoidDeath = new HashMap<>();

    public static final HashMap<UUID, HashMap<UUID, Long>> rateLimit = new HashMap<>();
    public static final int RATE_LIMIT_TIME = 5000;

    static int lastCheckedSeconds = secondsUntilNextUtcMidnight() - 1;
    static boolean didMinBroadcast = false;
    static boolean didFiveMinBroadcast = false;
    static boolean didTenMinBroadcast = false;
    static long gracePeriod = LocalTime.now().toEpochSecond(LocalDate.EPOCH, ZoneOffset.UTC) + 180;
    static boolean graceLog = false;

    static HashSet<UUID> justJoined = new HashSet<>();
    static HashSet<UUID> justChecked = new HashSet<>();

    public static final HashMap<String, Pair<String, String>> memberList = new HashMap<>() {{
        put("Mono", Pair.of("TheMonHub", "DakariMona"));
        put("Sasha", Pair.of("Tsar_AlexanderII", "DailyWire1960"));
        put("Ziggy", Pair.of("HumbleCreator101", null));
        put("Ash", Pair.of("Niraxes", null));
        put("Max", Pair.of("Brasskicker69", null));
        put("Leo", Pair.of(null, "mozzamoonart"));
        put("Kip", Pair.of("subwayboi", null));
        put("Torrey", Pair.of("Ax0lotls1519", "Axolotllabs"));
        put("Bommel", Pair.of("bommelnator", "blockflurry"));
        put("Nyxion", Pair.of("NyxionUwU", "FoxDefender"));
        put("Rushb", Pair.of(null, "rushb1331uwu"));
        put("MinMin", Pair.of(null, "Ledonaceitunio"));
        put("Ibm", Pair.of(null, "SpectralOne400"));
        put("Bug", Pair.of("zigzag183", null));
        put("Klipp", Pair.of("klipp3k", "klipp3k"));
    }};

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
        ServerPlayerEvents.JOIN.register(Ftsmp::onJoin);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("ftsmp")
                .then(Commands.literal("help")
                        .executes(ctx -> executeHelpCommand(ctx, dispatcher))
                )
                .then(Commands.literal("version").executes(Ftsmp::executeVersionCommand))
                .then(Commands.literal("time").executes(Ftsmp::executeTimeCommand))
                .then(Commands.literal("send").then(Commands.argument("targetPlayer", GameProfileArgument.gameProfile()).then(Commands.argument("message", StringArgumentType.string()).executes(Ftsmp::executeNoticeCommand))))
                .then(Commands.literal("check").executes(Ftsmp::executeCheckCommand))
                .then(Commands.literal("clear").executes(Ftsmp::executeClearCommand))
                .then(Commands.literal("list").executes(Ftsmp::executeListCommand))
        ));
    }

    private static int executeListCommand(CommandContext<CommandSourceStack> commandContext) {
        var source = commandContext.getSource();

        source.sendSuccess(() -> Component.literal("The members of the furryteens SMP are:"), false);
        for (String nickname : memberList.keySet()) {
            Pair<String, String> names = memberList.get(nickname);
            String java = names.getSecond() != null ? " (Java username: " + names.getSecond() + ")" : "";
            String bedrock = names.getFirst() != null ? " (Bedrock username: " + names.getFirst() + ")" : "";
            source.sendSuccess(() -> Component.literal("- " + nickname + java + bedrock), false);
        }
        return 1;
    }

    private static void onJoin(ServerPlayer serverPlayer) {
        justJoined.add(serverPlayer.getUUID());
    }

    public static String getVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("UNKNOWN");
    }

    private static int executeVersionCommand(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        commandSourceStackCommandContext.getSource().sendSuccess(() -> Component.literal("FtSMP version: §l" + getVersion()), false);
        return 1;
    }

    private static int executeHelpCommand(CommandContext<CommandSourceStack> context, CommandDispatcher<CommandSourceStack> dispatcher) {
        var source = context.getSource();
        var node = dispatcher.getRoot().getChild("ftsmp");
        executeVersionCommand(context);
        source.sendSuccess(() -> Component.literal("Available commands:"), false);
        dispatcher.getSmartUsage(node, source).forEach((k, var) ->
                source.sendSuccess(() -> Component.literal("/ftsmp " + var), false)
        );
        return 1;
    }

    private static int executeTimeCommand(CommandContext<CommandSourceStack> commandContext) {
        commandContext.getSource().sendSuccess(() -> Component.literal("Time before shutdown in seconds: §l" + secondsUntilNextUtcMidnight()), false);
        return 1;
    }

    private static void runGreet(MinecraftServer server, UUID player) {
        Map<String, LinkedHashSet<String>> noticed = PersistentData.get(server).getNoticed(player);
        ServerPlayer player1 = server.getPlayerList().getPlayer(player);
        if (player1 == null) {
            return;
        }
        if (GeyserApi.api().connectionByUuid(player) != null) {
            player1.sendSystemMessage(Component.literal("§c§lMIGRATION: Please link your bedrock and java accounts using this link: ").append(
                    Component.literal("https://link.geysermc.org")
                            .setStyle(Style.EMPTY.withClickEvent(
                                    new ClickEvent.OpenUrl(URI.create("https://link.geysermc.org"))
                            ))
            ));
            player1.sendSystemMessage(Component.literal("§c§In the future, any players (with a bedrock account specified in the member list) without linked accounts will not be able to join the server!"));
        }
        if (!noticed.isEmpty()) {
            player1.sendSystemMessage(Component.literal("§eYou got " + noticed.size() + " messages left unread."));
            player1.sendSystemMessage(
                    Component.literal("§e§lRun ")
                            .append(
                                    Component.literal("§n/ftsmp check§r§e§l")
                                            .setStyle(Style.EMPTY.withClickEvent(
                                                    new ClickEvent.RunCommand("/ftsmp check")
                                            ))
                            )
                            .append(Component.literal(" to check them out!")),
                    false
            );
        }
        if (noticed.size() >= 50) {
            player1.sendSystemMessage(Component.literal("§c§lYour message inbox is full! Please check and clear your messages to receive new ones."));
        }
    }

    private static void runNotice(MinecraftServer server, UUID player) {
        Map<String, LinkedHashSet<String>> noticed = PersistentData.get(server).getNoticed(player);
        ServerPlayer player1 = server.getPlayerList().getPlayer(player);
        if (player1 == null) {
            return;
        }
        if (noticed.isEmpty()) {
            player1.sendSystemMessage(Component.literal("You don't have any message left unread!"));
            return;
        }
        for (String name : noticed.keySet()) {
            player1.sendSystemMessage(Component.literal("From " + name + ":"));
            player1.sendSystemMessage(Component.literal("§l----------"));
            for (String msg : noticed.get(name)) {
                player1.sendSystemMessage(Component.literal("§o" + msg));
                player1.sendSystemMessage(Component.literal("§l=========="));
            }
        }
        player1.sendSystemMessage(
                Component.literal("§e§lRun ")
                        .append(
                                Component.literal("§n/ftsmp clear§r§e§l")
                                        .setStyle(Style.EMPTY.withClickEvent(
                                                new ClickEvent.RunCommand("/ftsmp clear")
                                        ))
                        )
                        .append(Component.literal(" to clear your inbox!")),
                false
        );
        PersistentData.get(server).clearNotices(player);
    }

    private static int executeClearCommand(CommandContext<CommandSourceStack> commandContext) {
        PersistentData.get(commandContext.getSource().getServer()).clearNotices(Objects.requireNonNull(commandContext.getSource().getPlayer()).getUUID());
        commandContext.getSource().sendSuccess(() -> Component.literal("Cleared your inbox!"), false);
        return 1;
    }

    private static int executeCheckCommand(CommandContext<CommandSourceStack> commandContext) {
        justChecked.add(Objects.requireNonNull(commandContext.getSource().getPlayer()).getUUID());
        commandContext.getSource().sendSuccess(() -> Component.literal("Checking inbox..."), false);
        return 1;
    }

    private static int executeNoticeCommand(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        long startTime = System.currentTimeMillis();
        UUID callerId = Objects.requireNonNull(commandContext.getSource().getPlayer()).getUUID();
        Component callerName = Objects.requireNonNull(commandContext.getSource().getPlayer()).getDisplayName();
        Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(commandContext, "targetPlayer");
        String msg = StringArgumentType.getString(commandContext, "message");

        for (NameAndId target : targets) {
            UUID uuid = target.id();
            if (uuid == null) {
                commandContext.getSource().sendFailure(
                        Component.literal("Could not resolve UUID for " + target.name())
                );
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
            }

            if (uuid == callerId) {
                commandContext.getSource().sendFailure(Component.literal("Why are you trying to message yourself...?"));
                return 1;
            }

            rateLimit.putIfAbsent(callerId, new HashMap<>());
            var map = rateLimit.get(callerId);
            if (map.containsKey(uuid)) {
                long time = map.get(uuid);
                if (time + RATE_LIMIT_TIME > startTime) {
                    commandContext.getSource().sendFailure(Component.literal("You have been rate limited!"));
                    commandContext.getSource().sendFailure(Component.literal("Please wait §l" + ((double) (time + RATE_LIMIT_TIME - startTime) / 1000) + " §rseconds before trying again."));
                    return 1;
                }
            }
            map.put(uuid, startTime);

            var dat = PersistentData.get(commandContext.getSource().getServer()).getNoticed(uuid);

            if (dat.get(callerName.getString()) != null) {
                if (dat.get(callerName.getString()).size() >= 10) {
                    commandContext.getSource().sendFailure(Component.literal("You have reached the maximum number of messages you can send without them checking!"));
                    return 1;
                }
            }

            if (dat.size() >= 50) {
                commandContext.getSource().sendFailure(Component.literal(target.name() + "'s message is full!"));
                return 1;
            }

            commandContext.getSource().sendSuccess(() -> Component.literal("Messaged " + target.name() + "§r!"), false);

            ServerPlayer targetPlayer = commandContext.getSource().getServer().getPlayerList().getPlayer(uuid);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal("§e§lYou got a message from " + callerName.getString() + "!"));
                targetPlayer.sendSystemMessage(
                        Component.literal("§e§lRun ")
                                .append(
                                        Component.literal("§n/ftsmp check§r§e§l")
                                                .setStyle(Style.EMPTY.withClickEvent(
                                                        new ClickEvent.RunCommand("/ftsmp check")
                                                ))
                                )
                                .append(Component.literal(" to check them out!")),
                        false
                );
            }
            PersistentData.get(commandContext.getSource().getServer()).addNoticedPlayer(uuid, callerName.getString(), msg);
            Ftsmp.LOGGER.info("MSG: Player {} sent message to {}: {}", callerName.getString(), target.name(), msg);
            return 1;
        }
        commandContext.getSource().sendFailure(Component.literal("No players were found!"));
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    }

    private static void onLeave(ServerPlayer player) {
        isVoidDeath.remove(player.getUUID());
    }

    private static void onTick(MinecraftServer minecraftServer) {
        int secondsLeft = secondsUntilNextUtcMidnight() - 1;

        for (UUID uuid : justJoined) {
            runGreet(minecraftServer, uuid);
            justJoined.remove(uuid);
        }
        for (UUID uuid : justChecked) {
            runNotice(minecraftServer, uuid);
            justChecked.remove(uuid);
        }

        if (Ftsmp.lastCheckedSeconds != secondsLeft) {
            lastCheckedSeconds = secondsLeft;

            if (gracePeriod > LocalTime.now().toEpochSecond(LocalDate.EPOCH, ZoneOffset.UTC)) {
                return;
            }
            if (!graceLog) {
                graceLog = true;
                Ftsmp.LOGGER.info("Grace period over, starting shutdown countdown.");
            }

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
