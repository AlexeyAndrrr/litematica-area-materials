package dev.areamaterials;

import java.util.ArrayList;
import java.util.List;

/** Inclusive integer bounds. Kept independent of Minecraft for geometry tests. */
public record IntBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public IntBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Inverted bounds");
    }

    public static IntBox between(int ax, int ay, int az, int bx, int by, int bz) {
        return new IntBox(Math.min(ax, bx), Math.min(ay, by), Math.min(az, bz),
                Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public long volume() {
        return ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1);
    }

    public IntBox intersection(IntBox other) {
        int x1 = Math.max(minX, other.minX), y1 = Math.max(minY, other.minY), z1 = Math.max(minZ, other.minZ);
        int x2 = Math.min(maxX, other.maxX), y2 = Math.min(maxY, other.maxY), z2 = Math.min(maxZ, other.maxZ);
        return x1 <= x2 && y1 <= y2 && z1 <= z2 ? new IntBox(x1, y1, z1, x2, y2, z2) : null;
    }

    /** At most six non-overlapping pieces, so overlapping subregions never count twice. */
    public List<IntBox> subtract(IntBox other) {
        IntBox cut = intersection(other);
        if (cut == null) return List.of(this);
        List<IntBox> result = new ArrayList<>(6);
        if (minX < cut.minX) result.add(new IntBox(minX, minY, minZ, cut.minX - 1, maxY, maxZ));
        if (cut.maxX < maxX) result.add(new IntBox(cut.maxX + 1, minY, minZ, maxX, maxY, maxZ));
        if (minY < cut.minY) result.add(new IntBox(cut.minX, minY, minZ, cut.maxX, cut.minY - 1, maxZ));
        if (cut.maxY < maxY) result.add(new IntBox(cut.minX, cut.maxY + 1, minZ, cut.maxX, maxY, maxZ));
        if (minZ < cut.minZ) result.add(new IntBox(cut.minX, cut.minY, minZ, cut.maxX, cut.maxY, cut.minZ - 1));
        if (cut.maxZ < maxZ) result.add(new IntBox(cut.minX, cut.minY, cut.maxZ + 1, cut.maxX, cut.maxY, maxZ));
        return result;
    }

    public static List<IntBox> union(List<IntBox> boxes) {
        List<IntBox> result = new ArrayList<>();
        for (IntBox box : boxes) {
            List<IntBox> pieces = List.of(box);
            for (IntBox existing : result) {
                List<IntBox> next = new ArrayList<>();
                for (IntBox piece : pieces) next.addAll(piece.subtract(existing));
                pieces = next;
                if (pieces.isEmpty()) break;
            }
            result.addAll(pieces);
        }
        return List.copyOf(result);
    }
}
