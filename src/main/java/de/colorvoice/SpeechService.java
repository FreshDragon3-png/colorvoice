package de.colorvoice;

import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/** Offline-Spracherkennung (Vosk) pro Spieler, begrenzt auf die Farbwörter. */
public final class SpeechService {
    private final List<Model> models = new ArrayList<>();
    private final String grammar;
    private final Map<UUID, List<Recognizer>> recognizers = new ConcurrentHashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "colorvoice-speech");
        t.setDaemon(true);
        return t;
    });
    private final BiConsumer<UUID, String> onColor;

    public SpeechService(Config cfg, BiConsumer<UUID, String> onColor) {
        this.onColor = onColor;
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        for (String path : new String[]{cfg.modelDe, cfg.modelEn}) {
            File f = new File(path);
            if (f.isDirectory()) {
                try { models.add(new Model(f.getAbsolutePath())); }
                catch (Exception | UnsatisfiedLinkError e) { ColorVoiceMod.LOGGER.error("Modell konnte nicht geladen werden: " + path, e); }
            } else {
                ColorVoiceMod.LOGGER.warn("Vosk-Modell nicht gefunden: {}", path);
            }
        }
        List<String> words = new ArrayList<>(ColorBlocks.WORDS.keySet());
        words.add("[unk]");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < words.size(); i++) sb.append(i > 0 ? "," : "").append('"').append(words.get(i)).append('"');
        this.grammar = sb.append("]").toString();
        ColorVoiceMod.LOGGER.info("ColorVoice: {} Sprachmodell(e) geladen", models.size());
    }

    /** pcm48k: 48-kHz-Mono-PCM aus dem Opus-Decoder. */
    public void feed(UUID player, short[] pcm48k) {
        if (models.isEmpty()) return;
        // 48 kHz -> 16 kHz (Mittelwert über je 3 Samples)
        int n = pcm48k.length / 3;
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int s = (pcm48k[i * 3] + pcm48k[i * 3 + 1] + pcm48k[i * 3 + 2]) / 3;
            out[i * 2] = (byte) (s & 0xff);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        worker.execute(() -> {
            List<Recognizer> list = recognizers.computeIfAbsent(player, id -> {
                List<Recognizer> l = new ArrayList<>();
                for (Model m : models) {
                    try { l.add(new Recognizer(m, 16000f, grammar)); } catch (Exception e) { ColorVoiceMod.LOGGER.error("Recognizer", e); }
                }
                return l;
            });
            for (Recognizer r : list) {
                if (r.acceptWaveForm(out, out.length)) {
                    String text = new JSONObject(r.getResult()).optString("text", "").toLowerCase();
                    String color = findColor(text);
                    if (color != null) onColor.accept(player, color);
                }
            }
        });
    }

    private static String findColor(String text) {
        if (text.isBlank()) return null;
        // längste Treffer zuerst ("light blue" vor "blue")
        String best = null;
        for (String w : ColorBlocks.WORDS.keySet()) {
            if (text.equals(w) || text.contains(w)) {
                if (best == null || w.length() > best.length()) best = w;
            }
        }
        return best;
    }

    public void remove(UUID player) {
        worker.execute(() -> {
            List<Recognizer> l = recognizers.remove(player);
            if (l != null) l.forEach(Recognizer::close);
        });
    }
}
