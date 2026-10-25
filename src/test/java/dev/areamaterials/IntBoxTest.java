package dev.areamaterials;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class IntBoxTest {
    @Test void reversedCornersAreInclusive() {
        var box = IntBox.between(4, 8, 12, 2, 7, 10);
        assertEquals(18, box.volume());
        assertTrue(box.contains(4, 8, 12));
        assertFalse(box.contains(5, 8, 12));
        assertEquals(1, IntBox.between(-3, -64, 2, -3, -64, 2).volume());
    }

    @Test void subtractionPreservesExactlyTheUncutBlocks() {
        Random random = new Random(42);
        for (int sample = 0; sample < 500; sample++) {
            IntBox source = box(random), cut = box(random);
            Set<String> expected = points(source);
            expected.removeAll(points(cut));
            Set<String> result = new HashSet<>();
            for (IntBox piece : source.subtract(cut)) {
                for (String point : points(piece)) assertTrue(result.add(point), "Overlapping subtraction pieces");
            }
            assertEquals(expected, result);
        }
    }

    @Test void unionDoesNotDoubleCountOverlappingRegions() {
        Random random = new Random(314);
        for (int sample = 0; sample < 150; sample++) {
            List<IntBox> boxes = new ArrayList<>();
            Set<String> expected = new HashSet<>();
            for (int i = 0; i < 8; i++) { var box = box(random); boxes.add(box); expected.addAll(points(box)); }
            Set<String> result = new HashSet<>();
            long volume = 0;
            for (IntBox piece : IntBox.union(boxes)) {
                volume += piece.volume();
                for (String point : points(piece)) assertTrue(result.add(point), "Duplicate block");
            }
            assertEquals(expected, result);
            assertEquals(expected.size(), volume);
        }
    }

    @Test void adjacentBoxesDoNotIntersect() {
        var a = new IntBox(-16, -64, 0, -1, 319, 15);
        var b = new IntBox(0, -64, 0, 15, 319, 15);
        assertNull(a.intersection(b));
        assertEquals(List.of(a), a.subtract(b));
    }

    private IntBox box(Random random) {
        return IntBox.between(random.nextInt(7)-3, random.nextInt(7)-3, random.nextInt(7)-3,
                random.nextInt(7)-3, random.nextInt(7)-3, random.nextInt(7)-3);
    }

    private Set<String> points(IntBox box) {
        Set<String> result = new HashSet<>();
        for (int x=box.minX();x<=box.maxX();x++) for (int y=box.minY();y<=box.maxY();y++) for (int z=box.minZ();z<=box.maxZ();z++)
            result.add(x+","+y+","+z);
        return result;
    }
}
