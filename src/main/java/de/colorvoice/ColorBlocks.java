package de.colorvoice;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/** Farbwort (Deutsch + Englisch, kleingeschrieben) -> Block. */
public final class ColorBlocks {
    public static final Map<String, Block> WORDS = new LinkedHashMap<>();

    private static void add(Block block, String... words) {
        for (String w : words) WORDS.put(w, block);
    }

    static {
        // Hier kannst du die Blöcke ändern, z.B. Blocks.NETHERRACK statt Blocks.RED_WOOL bei "rot"
        add(Blocks.RED_WOOL, "rot", "red");
        add(Blocks.BLUE_WOOL, "blau", "blue");
        add(Blocks.GREEN_WOOL, "grün", "green");
        add(Blocks.YELLOW_WOOL, "gelb", "yellow");
        add(Blocks.ORANGE_WOOL, "orange");
        add(Blocks.PURPLE_WOOL, "lila", "violett", "purple");
        add(Blocks.PINK_WOOL, "pink", "rosa");
        add(Blocks.WHITE_WOOL, "weiß", "weiss", "white");
        add(Blocks.BLACK_WOOL, "schwarz", "black");
        add(Blocks.GRAY_WOOL, "grau", "gray", "grey");
        add(Blocks.BROWN_WOOL, "braun", "brown");
        add(Blocks.CYAN_WOOL, "türkis", "cyan");
        add(Blocks.LIGHT_BLUE_WOOL, "hellblau", "light blue");
        add(Blocks.LIME_WOOL, "hellgrün", "lime");
        add(Blocks.LIGHT_GRAY_WOOL, "hellgrau", "light gray");
        add(Blocks.MAGENTA_WOOL, "magenta");
    }

    private ColorBlocks() {}
}
