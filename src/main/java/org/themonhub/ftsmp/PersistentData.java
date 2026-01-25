package org.themonhub.ftsmp;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class PersistentData extends SavedData {

    private final HashMap<UUID, HashMap<String, LinkedHashSet<String>>> noticedPlayers;

    private static final Codec<LinkedHashSet<String>> STRING_SET_CODEC =
            Codec.STRING.listOf().xmap(LinkedHashSet::new, ArrayList::new);

    private static final Codec<HashMap<String, LinkedHashSet<String>>> INNER_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, STRING_SET_CODEC)
                    .xmap(HashMap::new, map -> map);

    private static final Codec<UUID> UUID_STRING_CODEC =
            Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<PersistentData> CODEC =
            Codec.unboundedMap(
                    UUID_STRING_CODEC,
                    INNER_MAP_CODEC
            ).xmap(
                    PersistentData::new,
                    data -> data.noticedPlayers
            );

    @SuppressWarnings("DataFlowIssue")
    private static final SavedDataType<PersistentData> TYPE =
            new SavedDataType<>(
                    "ftsmp_data",
                    PersistentData::new,
                    CODEC,
                    null
            );

    public PersistentData() {
        this.noticedPlayers = new HashMap<>();
    }

    private PersistentData(Map<UUID, HashMap<String, LinkedHashSet<String>>> map) {
        this.noticedPlayers = new HashMap<>(map);
    }

    public static PersistentData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) {
            return new PersistentData();
        }
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addNoticedPlayer(UUID player, String category, String noticeMessage) {
        noticedPlayers
                .computeIfAbsent(player, k -> new HashMap<>())
                .computeIfAbsent(category, k -> new LinkedHashSet<>())
                .add(noticeMessage);
        setDirty();
    }

    public void clearNotices(UUID player) {
        noticedPlayers
                .computeIfAbsent(player, k -> new HashMap<>())
                .clear();
        setDirty();
    }

    public Map<String, LinkedHashSet<String>> getNoticed(UUID player) {
        HashMap<String, LinkedHashSet<String>> map = noticedPlayers.get(player);
        if (map == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }
}