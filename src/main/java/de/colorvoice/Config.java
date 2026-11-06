package de.colorvoice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    /** Ordner mit dem entpackten deutschen Vosk-Modell (z.B. vosk-model-small-de-0.15) */
    public String modelDe = "config/colorvoice/vosk-model-small-de-0.15";
    /** Ordner mit dem entpackten englischen Vosk-Modell (z.B. vosk-model-small-en-us-0.15) */
    public String modelEn = "config/colorvoice/vosk-model-small-en-us-0.15";
    /** Radius in Blöcken (Meter) */
    public int radius = 100;
    /** Wartezeit in Sekunden zwischen zwei Verwandlungen pro Spieler */
    public int cooldownSeconds = 3;
    /** Nur diese Spieler (Namen) dürfen auslösen. Leer = alle. */
    public List<String> allowedPlayers = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("colorvoice.json");
        try {
            if (Files.exists(file)) {
                return GSON.fromJson(Files.readString(file), Config.class);
            }
            Config c = new Config();
            Files.writeString(file, GSON.toJson(c));
            return c;
        } catch (Exception e) {
            ColorVoiceMod.LOGGER.error("Konnte Config nicht laden, nutze Standard", e);
            return new Config();
        }
    }
}
