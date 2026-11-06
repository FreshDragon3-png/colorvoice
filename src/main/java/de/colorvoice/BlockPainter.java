package de.colorvoice;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Ersetzt Blöcke kugelförmig um einen Punkt, verteilt über mehrere Ticks, damit der Server nicht einfriert. */
public final class BlockPainter {
    private static final long BUDGET_NANOS = 25_000_000L; // ~25 ms pro Tick
    private static Job current;

    public static boolean busy() { return current != null; }

    public static void start(ServerLevel level, BlockPos center, int radius, Block block) {
        if (current != null) return;
        current = new Job(level, center, radius, block);
    }

    /** Wird jeden Server-Tick aufgerufen. */
    public static void tick() {
        if (current == null) return;
        long end = System.nanoTime() + BUDGET_NANOS;
        while (System.nanoTime() < end && current.hasNext()) current.processNextChunk();
        if (!current.hasNext()) current = null;
    }

    private static final class Job {
        final ServerLevel level;
        final BlockPos center;
        final int r;
        final BlockState target;
        final Iterator<int[]> chunks;

        Job(ServerLevel level, BlockPos center, int r, Block block) {
            this.level = level;
            this.center = center;
            this.r = r;
            this.target = block.defaultBlockState();
            List<int[]> list = new ArrayList<>();
            for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++)
                for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++)
                    list.add(new int[]{cx, cz});
            // nahe Chunks zuerst
            list.sort(Comparator.comparingLong(c -> {
                long dx = (c[0] << 4) + 8 - center.getX(), dz = (c[1] << 4) + 8 - center.getZ();
                return dx * dx + dz * dz;
            }));
            this.chunks = list.iterator();
        }

        boolean hasNext() { return chunks.hasNext(); }

        void processNextChunk() {
            int[] c = chunks.next();
            if (!level.hasChunk(c[0], c[1])) return; // keine neuen Chunks laden
            long r2 = (long) r * r;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = (c[0] << 4) + lx, z = (c[1] << 4) + lz;
                    long dx = x - center.getX(), dz = z - center.getZ();
                    long rem = r2 - dx * dx - dz * dz;
                    if (rem < 0) continue;
                    int dy = (int) Math.floor(Math.sqrt(rem));
                    int y0 = Math.max(level.getMinY(), center.getY() - dy);
                    int y1 = Math.min(level.getMaxY() - 1, center.getY() + dy);
                    for (int y = y0; y <= y1; y++) {
                        pos.set(x, y, z);
                        BlockState s = level.getBlockState(pos);
                        if (s.isAir() || s.hasBlockEntity() || s.is(target.getBlock())) continue;
                        level.setBlock(pos, target, Block.UPDATE_CLIENTS); // ohne Nachbar-Updates -> keine Drops
                    }
                }
            }
        }
    }

    private BlockPainter() {}
}
