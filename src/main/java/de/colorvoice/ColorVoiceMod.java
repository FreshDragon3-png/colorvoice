package de.colorvoice;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ColorVoiceMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("colorvoice");
    public static volatile SpeechService speech;
    private static volatile MinecraftServer server;
    private static Config config;
    private static final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        config = Config.load();
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            try {
                speech = new SpeechService(config, ColorVoiceMod::onColorHeard);
            } catch (Throwable t) {
                LOGGER.error("Spracherkennung konnte nicht gestartet werden", t);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> { speech = null; server = null; });
        ServerTickEvents.END_SERVER_TICK.register(s -> BlockPainter.tick());
    }

    /** Läuft auf dem Sprach-Thread -> auf den Server-Thread wechseln. */
    private static void onColorHeard(UUID id, String word) {
        MinecraftServer s = server;
        if (s == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) return;
            if (!config.allowedPlayers.isEmpty() && !config.allowedPlayers.contains(p.getGameProfile().name())) return;
            long now = System.currentTimeMillis();
            if (BlockPainter.busy() || now - lastUse.getOrDefault(id, 0L) < config.cooldownSeconds * 1000L) return;
            Block block = ColorBlocks.WORDS.get(word);
            if (block == null) return;
            lastUse.put(id, now);
            p.sendSystemMessage(Component.literal("Farbe erkannt: " + word));
            BlockPainter.start((ServerLevel) p.level(), p.blockPosition(), config.radius, block);
        });
    }
}
